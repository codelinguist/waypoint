package com.waypoint.position;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.waypoint.household.Asset;
import com.waypoint.household.AssetType;
import com.waypoint.household.Household;
import com.waypoint.household.HouseholdRepository;
import com.waypoint.household.Liquidity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Exercises {@code GET /api/households/{householdId}/financial-position}
 * against real PostgreSQL: exact decimal-string reconciliation, ordering,
 * provenance, and error/isolation behavior. Fixture creation reuses the
 * existing household/asset/liability/snapshot REST endpoints read/write as
 * any other API caller would, without editing their production code.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class FinancialPositionApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private HouseholdRepository householdRepository;

    @Autowired
    private PositionAssetRepository assetRepository;

    @Test
    void reconcilesMixedCurrencyAssetsAndLiabilitiesFromPlanningValueNotEstimatedValue() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        createAsset(householdId, "Urban Lot", "PROPERTY", "3000000.00", "2500000.00", "PHP", "LIQUID");
        createAsset(householdId, "Brokerage", "INVESTMENT", "200.00", "200.00", "USD", "LIQUID");
        createLiability(householdId, "Mortgage", "MORTGAGE", "500000.00", "PHP");
        createLiability(householdId, "Card", "CREDIT_CARD", "50.00", "USD");

        mockMvc.perform(get("/api/households/{h}/financial-position", householdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalsByCurrency.length()").value(2))
                .andExpect(jsonPath("$.totalsByCurrency[0].currency").value("PHP"))
                .andExpect(jsonPath("$.totalsByCurrency[0].assetTotal").value("2500000.00"))
                .andExpect(jsonPath("$.totalsByCurrency[0].liabilityTotal").value("500000.00"))
                .andExpect(jsonPath("$.totalsByCurrency[0].netWorth").value("2000000.00"))
                .andExpect(jsonPath("$.totalsByCurrency[1].currency").value("USD"))
                .andExpect(jsonPath("$.totalsByCurrency[1].assetTotal").value("200.00"))
                .andExpect(jsonPath("$.totalsByCurrency[1].liabilityTotal").value("50.00"))
                .andExpect(jsonPath("$.totalsByCurrency[1].netWorth").value("150.00"));
    }

    @Test
    void assetOnlyHouseholdHasExactZeroLiabilityTotalAndNoAllCurrencySum() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        createAsset(householdId, "Cash", "CASH", "100.00", "100.00", "PHP", "LIQUID");

        mockMvc.perform(get("/api/households/{h}/financial-position", householdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assets.length()").value(1))
                .andExpect(jsonPath("$.liabilities.length()").value(0))
                .andExpect(jsonPath("$.totalsByCurrency.length()").value(1))
                .andExpect(jsonPath("$.totalsByCurrency[0].liabilityTotal").value("0.00"))
                .andExpect(jsonPath("$.totalsByCurrency[0].netWorth").value("100.00"));
    }

    @Test
    void liabilityOnlyHouseholdHasExactZeroAssetTotal() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        createLiability(householdId, "Loan", "PERSONAL_LOAN", "1000.00", "PHP");

        mockMvc.perform(get("/api/households/{h}/financial-position", householdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assets.length()").value(0))
                .andExpect(jsonPath("$.liabilities.length()").value(1))
                .andExpect(jsonPath("$.totalsByCurrency[0].assetTotal").value("0.00"))
                .andExpect(jsonPath("$.totalsByCurrency[0].netWorth").value("-1000.00"));
    }

    @Test
    void emptyHouseholdHasEmptyListsAndNoInventedZeroGroup() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        mockMvc.perform(get("/api/households/{h}/financial-position", householdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assets").isArray())
                .andExpect(jsonPath("$.assets.length()").value(0))
                .andExpect(jsonPath("$.liabilities.length()").value(0))
                .andExpect(jsonPath("$.totalsByCurrency").isArray())
                .andExpect(jsonPath("$.totalsByCurrency.length()").value(0));
    }

    @Test
    void negativeNetWorthWhenLiabilitiesExceedAssets() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        createAsset(householdId, "Cash", "CASH", "100.00", "100.00", "PHP", "LIQUID");
        createLiability(householdId, "Loan", "PERSONAL_LOAN", "500.00", "PHP");

        mockMvc.perform(get("/api/households/{h}/financial-position", householdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalsByCurrency[0].netWorth").value("-400.00"));
    }

    @Test
    void zeroValuedSourceRowsRemainIncluded() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        createAsset(householdId, "Empty Wallet", "CASH", "0.00", "0.00", "PHP", "LIQUID");
        createLiability(householdId, "Paid Off Card", "CREDIT_CARD", "0.00", "PHP");

        mockMvc.perform(get("/api/households/{h}/financial-position", householdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assets.length()").value(1))
                .andExpect(jsonPath("$.assets[0].planningValue").value("0.00"))
                .andExpect(jsonPath("$.liabilities.length()").value(1))
                .andExpect(jsonPath("$.liabilities[0].outstandingBalance").value("0.00"))
                .andExpect(jsonPath("$.totalsByCurrency[0].assetTotal").value("0.00"))
                .andExpect(jsonPath("$.totalsByCurrency[0].liabilityTotal").value("0.00"))
                .andExpect(jsonPath("$.totalsByCurrency[0].netWorth").value("0.00"));
    }

    @Test
    void aboveIndividualStoragePrecisionValuesRoundTripAsExactTwoDecimalStringsWithNoTruncation() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        String maxStorable = "99999999999999999.99";
        createAsset(householdId, "Fund A", "INVESTMENT", maxStorable, maxStorable, "PHP", "LIQUID");
        createAsset(householdId, "Fund B", "INVESTMENT", maxStorable, maxStorable, "PHP", "LIQUID");

        String body = mockMvc.perform(get("/api/households/{h}/financial-position", householdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assets[0].planningValue").value(maxStorable))
                .andExpect(jsonPath("$.assets[1].planningValue").value(maxStorable))
                .andReturn().getResponse().getContentAsString();

        JsonNode totals = objectMapper.readTree(body).get("totalsByCurrency").get(0);
        String assetTotal = totals.get("assetTotal").asText();
        assertNoScientificNotation(assetTotal);
        assertThat(new BigDecimal(assetTotal)).isEqualByComparingTo(new BigDecimal("199999999999999999.98"));
        assertThat(totals.get("netWorth").asText()).isEqualTo(assetTotal);
    }

    @Test
    void preservesSourceDatesLiquidityAndProvenance() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        createAsset(householdId, "Emergency Fund", "CASH", "500.00", "500.00", "PHP", "LIQUID");

        mockMvc.perform(get("/api/households/{h}/financial-position", householdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assets[0].valuedAt").value(LocalDate.now().toString()))
                .andExpect(jsonPath("$.assets[0].liquidity").value("LIQUID"))
                .andExpect(jsonPath("$.assets[0].sourceType").value("MANUAL_ENTRY"))
                .andExpect(jsonPath("$.householdName").value("Ralph Household"))
                .andExpect(jsonPath("$.baseCurrency").value("PHP"));
    }

    @Test
    void returnsFutureDatedRowsExplicitlyRatherThanFilteringThemLikeSnapshotCreationDoes() throws Exception {
        // The create-asset endpoint itself rejects a future valuedAt (@PastOrPresent), so a
        // future-dated row is persisted directly, the way an import or a later edit flow could
        // produce one; this proves the read model does not filter by date the way
        // FinancialSnapshotService's eligibility check does for snapshot creation.
        Household household = householdRepository.save(new Household("Future Household", "PHP"));
        LocalDate futureDate = LocalDate.now().plusDays(30);
        assetRepository.save(new Asset(
                household, "Future Bonus", AssetType.CASH, new BigDecimal("500.00"), new BigDecimal("500.00"),
                "PHP", futureDate, Liquidity.LIQUID));

        mockMvc.perform(get("/api/households/{h}/financial-position", household.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assets.length()").value(1))
                .andExpect(jsonPath("$.assets[0].valuedAt").value(futureDate.toString()));
    }

    @Test
    void returnsNotFoundForUnknownHousehold() throws Exception {
        mockMvc.perform(get("/api/households/{h}/financial-position", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("HOUSEHOLD_NOT_FOUND"));
    }

    @Test
    void returnsBadRequestForMalformedHouseholdIdentifier() throws Exception {
        mockMvc.perform(get("/api/households/{h}/financial-position", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));
    }

    @Test
    void keepsAssetsAndLiabilitiesIsolatedBetweenHouseholds() throws Exception {
        String householdOneId = createHouseholdId("Household One", "PHP");
        String householdTwoId = createHouseholdId("Household Two", "PHP");
        createAsset(householdOneId, "Fund A", "CASH", "1.00", "1.00", "PHP", "LIQUID");
        createLiability(householdOneId, "Loan A", "PERSONAL_LOAN", "1.00", "PHP");
        createAsset(householdTwoId, "Fund B", "CASH", "2.00", "2.00", "PHP", "LIQUID");

        mockMvc.perform(get("/api/households/{h}/financial-position", householdOneId))
                .andExpect(jsonPath("$.assets.length()").value(1))
                .andExpect(jsonPath("$.assets[0].name").value("Fund A"))
                .andExpect(jsonPath("$.liabilities.length()").value(1));
        mockMvc.perform(get("/api/households/{h}/financial-position", householdTwoId))
                .andExpect(jsonPath("$.assets.length()").value(1))
                .andExpect(jsonPath("$.assets[0].name").value("Fund B"))
                .andExpect(jsonPath("$.liabilities.length()").value(0));
    }

    @Test
    void doesNotWriteAnyRecordOrCreateASnapshot() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        createAsset(householdId, "Cash", "CASH", "100.00", "100.00", "PHP", "LIQUID");

        mockMvc.perform(get("/api/households/{h}/financial-position", householdId)).andExpect(status().isOk());
        mockMvc.perform(get("/api/households/{h}/financial-position", householdId)).andExpect(status().isOk());

        mockMvc.perform(get("/api/households/{h}/financial-snapshots", householdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/households/{h}/assets", householdId))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void returnsRowsDeterministicallyOrderedByUuidWithinKind() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        for (int i = 0; i < 5; i++) {
            createAsset(householdId, "Fund " + i, "CASH", "1.00", "1.00", "PHP", "LIQUID");
            createLiability(householdId, "Loan " + i, "PERSONAL_LOAN", "1.00", "PHP");
        }

        String body = mockMvc.perform(get("/api/households/{h}/financial-position", householdId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(body);

        assertOrderedByIdAscending(root.get("assets"));
        assertOrderedByIdAscending(root.get("liabilities"));

        // Ordering is stable across repeated reads of the same rows, not incidental to one call.
        String secondBody = mockMvc.perform(get("/api/households/{h}/financial-position", householdId))
                .andReturn().getResponse().getContentAsString();
        JsonNode secondRoot = objectMapper.readTree(secondBody);
        assertThat(idsOf(secondRoot.get("assets"))).isEqualTo(idsOf(root.get("assets")));
        assertThat(idsOf(secondRoot.get("liabilities"))).isEqualTo(idsOf(root.get("liabilities")));
    }

    private List<String> idsOf(JsonNode rows) {
        List<String> ids = new ArrayList<>();
        rows.forEach(row -> ids.add(row.get("id").asText()));
        return ids;
    }

    private void assertOrderedByIdAscending(JsonNode rows) {
        List<String> ids = idsOf(rows);
        List<String> sorted = new ArrayList<>(ids);
        sorted.sort(String::compareTo);
        assertThat(ids).isEqualTo(sorted);
    }

    private void assertNoScientificNotation(String value) {
        assertThat(value).doesNotContainIgnoringCase("e");
    }

    private String createHouseholdId(String name, String baseCurrency) throws Exception {
        String body = objectMapper.writeValueAsString(new HashMap<>() {{
            put("name", name);
            put("baseCurrency", baseCurrency);
        }});
        String response = mockMvc.perform(post("/api/households")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private void createAsset(
            String householdId, String name, String assetType, String estimatedValue, String planningValue,
            String currency, String liquidity
    ) throws Exception {
        createAssetWithDate(
                householdId, name, assetType, estimatedValue, planningValue, currency, liquidity,
                LocalDate.now().toString());
    }

    private void createAssetWithDate(
            String householdId, String name, String assetType, String estimatedValue, String planningValue,
            String currency, String liquidity, String valuedAt
    ) throws Exception {
        String body = objectMapper.writeValueAsString(new HashMap<>() {{
            put("name", name);
            put("assetType", assetType);
            put("estimatedValue", estimatedValue);
            put("planningValue", planningValue);
            put("currency", currency);
            put("valuedAt", valuedAt);
            put("liquidity", liquidity);
        }});
        ResultActions result = mockMvc.perform(post("/api/households/{h}/assets", householdId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
        result.andExpect(status().isCreated());
    }

    private void createLiability(
            String householdId, String name, String liabilityType, String outstandingBalance, String currency
    ) throws Exception {
        String body = objectMapper.writeValueAsString(new HashMap<>() {{
            put("name", name);
            put("liabilityType", liabilityType);
            put("outstandingBalance", outstandingBalance);
            put("currency", currency);
            put("balanceAsOf", LocalDate.now().toString());
        }});
        mockMvc.perform(post("/api/households/{h}/liabilities", householdId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }
}
