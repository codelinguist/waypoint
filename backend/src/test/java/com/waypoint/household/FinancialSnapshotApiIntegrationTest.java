package com.waypoint.household;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
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
 * Exercises the financial-snapshot API against a real PostgreSQL instance so
 * the Task 004 -> Task 005 Flyway upgrade path, JPA mapping, and REST
 * boundary are verified together.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class FinancialSnapshotApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createsSnapshotWithEligibleAssetAndLiability() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        createAsset(householdId, "Emergency Fund", "CASH", "1000.00", "PHP", LocalDate.now().toString(), "LIQUID");
        createLiability(householdId, "Credit Card", "CREDIT_CARD", "300.00", "PHP", LocalDate.now().toString());

        createSnapshot(householdId, LocalDate.now().toString())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.householdId").value(householdId))
                .andExpect(jsonPath("$.asOfDate").value(LocalDate.now().toString()))
                .andExpect(jsonPath("$.sourceType").value("MANUAL_ENTRY"))
                .andExpect(jsonPath("$.assetLineItems.length()").value(1))
                .andExpect(jsonPath("$.assetLineItems[0].name").value("Emergency Fund"))
                .andExpect(jsonPath("$.assetLineItems[0].value").value(1000.00))
                .andExpect(jsonPath("$.liabilityLineItems.length()").value(1))
                .andExpect(jsonPath("$.liabilityLineItems[0].name").value("Credit Card"))
                .andExpect(jsonPath("$.liabilityLineItems[0].value").value(300.00))
                .andExpect(jsonPath("$.totalsByCurrency.length()").value(1))
                .andExpect(jsonPath("$.totalsByCurrency[0].currency").value("PHP"))
                .andExpect(jsonPath("$.totalsByCurrency[0].assetTotal").value(1000.00))
                .andExpect(jsonPath("$.totalsByCurrency[0].liabilityTotal").value(300.00))
                .andExpect(jsonPath("$.totalsByCurrency[0].netWorth").value(700.00));
    }

    @Test
    void excludesRecordsDatedAfterAsOfDateAndIncludesRecordsDatedOnIt() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        LocalDate asOfDate = LocalDate.now().minusDays(1);
        createAsset(householdId, "Included", "CASH", "100.00", "PHP", asOfDate.toString(), "LIQUID");
        createAsset(householdId, "Excluded", "CASH", "200.00", "PHP", LocalDate.now().toString(), "LIQUID");

        createSnapshot(householdId, asOfDate.toString())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assetLineItems.length()").value(1))
                .andExpect(jsonPath("$.assetLineItems[0].name").value("Included"));
    }

    @Test
    void returnsEmptyZeroTotalSnapshotForHouseholdWithNoEligibleRecords() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createSnapshot(householdId, LocalDate.now().toString())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assetLineItems").isArray())
                .andExpect(jsonPath("$.assetLineItems.length()").value(0))
                .andExpect(jsonPath("$.liabilityLineItems.length()").value(0))
                .andExpect(jsonPath("$.totalsByCurrency.length()").value(0));
    }

    @Test
    void copiesSourceIdentityAndPreservesExactDecimalValue() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        String assetBody = createAsset(householdId, "Fund", "CASH", "1234.56", "PHP", LocalDate.now().toString(),
                "LIQUID").andReturn().getResponse().getContentAsString();
        String assetId = objectMapper.readTree(assetBody).get("id").asText();

        createSnapshot(householdId, LocalDate.now().toString())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assetLineItems[0].sourceAssetId").value(assetId))
                .andExpect(jsonPath("$.assetLineItems[0].value").value(1234.56))
                .andExpect(jsonPath("$.assetLineItems[0].currency").value("PHP"))
                .andExpect(jsonPath("$.assetLineItems[0].assetType").value("CASH"))
                .andExpect(jsonPath("$.assetLineItems[0].id").value(org.hamcrest.Matchers.not(assetId)));
    }

    @Test
    void computesNegativeNetWorthWhenLiabilitiesExceedAssets() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        createAsset(householdId, "Small Fund", "CASH", "50.00", "PHP", LocalDate.now().toString(), "LIQUID");
        createLiability(householdId, "Big Loan", "PERSONAL_LOAN", "200.00", "PHP", LocalDate.now().toString());

        createSnapshot(householdId, LocalDate.now().toString())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalsByCurrency[0].netWorth").value(-150.00));
    }

    @Test
    void keepsCurrenciesSeparateInTotals() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        createAsset(householdId, "PHP Cash", "CASH", "100.00", "PHP", LocalDate.now().toString(), "LIQUID");
        createAsset(householdId, "USD Cash", "CASH", "50.00", "USD", LocalDate.now().toString(), "LIQUID");
        createLiability(householdId, "PHP Loan", "PERSONAL_LOAN", "30.00", "PHP", LocalDate.now().toString());

        createSnapshot(householdId, LocalDate.now().toString())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalsByCurrency.length()").value(2))
                .andExpect(jsonPath("$.totalsByCurrency[0].currency").value("PHP"))
                .andExpect(jsonPath("$.totalsByCurrency[0].assetTotal").value(100.00))
                .andExpect(jsonPath("$.totalsByCurrency[0].liabilityTotal").value(30.00))
                .andExpect(jsonPath("$.totalsByCurrency[0].netWorth").value(70.00))
                .andExpect(jsonPath("$.totalsByCurrency[1].currency").value("USD"))
                .andExpect(jsonPath("$.totalsByCurrency[1].assetTotal").value(50.00))
                .andExpect(jsonPath("$.totalsByCurrency[1].liabilityTotal").value(0))
                .andExpect(jsonPath("$.totalsByCurrency[1].netWorth").value(50.00));
    }

    @Test
    void rejectsFutureAsOfDate() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createSnapshot(householdId, LocalDate.now().plusDays(1).toString())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsMissingAsOfDate() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        mockMvc.perform(post("/api/households/{h}/financial-snapshots", householdId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsSnapshotWithUnsupportedSourceTypeField() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        String body = """
                { "asOfDate": "%s", "sourceType": "IMPORTED" }
                """.formatted(LocalDate.now());

        mockMvc.perform(post("/api/households/{h}/financial-snapshots", householdId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsCreatingSnapshotForUnknownHousehold() throws Exception {
        createSnapshot(UUID.randomUUID().toString(), LocalDate.now().toString())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("HOUSEHOLD_NOT_FOUND"));
    }

    @Test
    void rejectsListingSnapshotsForUnknownHousehold() throws Exception {
        mockMvc.perform(get("/api/households/{h}/financial-snapshots", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("HOUSEHOLD_NOT_FOUND"));
    }

    @Test
    void returnsNotFoundWhenSnapshotBelongsToAnotherHousehold() throws Exception {
        String householdOneId = createHouseholdId("Household One", "PHP");
        String householdTwoId = createHouseholdId("Household Two", "PHP");

        String body = createSnapshot(householdOneId, LocalDate.now().toString())
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String snapshotId = objectMapper.readTree(body).get("id").asText();

        mockMvc.perform(get("/api/households/{h}/financial-snapshots/{s}", householdTwoId, snapshotId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("FINANCIAL_SNAPSHOT_NOT_FOUND"));
    }

    @Test
    void newHouseholdHasEmptySnapshotList() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        mockMvc.perform(get("/api/households/{h}/financial-snapshots", householdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void permitsDuplicateAsOfDateSnapshotsAndListsInAscendingOrder() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        LocalDate earlier = LocalDate.now().minusDays(1);
        LocalDate later = LocalDate.now();

        createSnapshot(householdId, later.toString()).andExpect(status().isCreated());
        createSnapshot(householdId, earlier.toString()).andExpect(status().isCreated());
        createSnapshot(householdId, earlier.toString()).andExpect(status().isCreated());

        mockMvc.perform(get("/api/households/{h}/financial-snapshots", householdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].asOfDate").value(earlier.toString()))
                .andExpect(jsonPath("$[1].asOfDate").value(earlier.toString()))
                .andExpect(jsonPath("$[2].asOfDate").value(later.toString()));
    }

    @Test
    void retrievesCreatedSnapshotById() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        createAsset(householdId, "Fund", "CASH", "10.00", "PHP", LocalDate.now().toString(), "LIQUID");

        String body = createSnapshot(householdId, LocalDate.now().toString())
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String snapshotId = objectMapper.readTree(body).get("id").asText();

        mockMvc.perform(get("/api/households/{h}/financial-snapshots/{s}", householdId, snapshotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(snapshotId))
                .andExpect(jsonPath("$.assetLineItems.length()").value(1));
    }

    @Test
    void keepsSnapshotsIsolatedBetweenHouseholds() throws Exception {
        String householdOneId = createHouseholdId("Household One", "PHP");
        String householdTwoId = createHouseholdId("Household Two", "PHP");
        createAsset(householdOneId, "Fund A", "CASH", "1", "PHP", LocalDate.now().toString(), "LIQUID");
        createAsset(householdTwoId, "Fund B", "CASH", "2", "PHP", LocalDate.now().toString(), "LIQUID");

        createSnapshot(householdOneId, LocalDate.now().toString()).andExpect(status().isCreated());
        createSnapshot(householdTwoId, LocalDate.now().toString()).andExpect(status().isCreated());

        mockMvc.perform(get("/api/households/{h}/financial-snapshots", householdOneId))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].assetLineItems[0].name").value("Fund A"));
        mockMvc.perform(get("/api/households/{h}/financial-snapshots", householdTwoId))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].assetLineItems[0].name").value("Fund B"));
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

    private ResultActions createAsset(
            String householdId, String name, String assetType, String estimatedValue, String currency,
            String valuedAt, String liquidity
    ) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", name);
        payload.put("assetType", assetType);
        payload.put("estimatedValue", estimatedValue);
        payload.put("planningValue", estimatedValue);
        payload.put("currency", currency);
        payload.put("valuedAt", valuedAt);
        payload.put("liquidity", liquidity);
        String body = objectMapper.writeValueAsString(payload);
        return mockMvc.perform(post("/api/households/{h}/assets", householdId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated());
    }

    private ResultActions createLiability(
            String householdId, String name, String liabilityType, String outstandingBalance, String currency,
            String balanceAsOf
    ) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", name);
        payload.put("liabilityType", liabilityType);
        payload.put("outstandingBalance", outstandingBalance);
        payload.put("currency", currency);
        payload.put("balanceAsOf", balanceAsOf);
        String body = objectMapper.writeValueAsString(payload);
        return mockMvc.perform(post("/api/households/{h}/liabilities", householdId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated());
    }

    private ResultActions createSnapshot(String householdId, String asOfDate) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("asOfDate", asOfDate);
        String body = objectMapper.writeValueAsString(payload);
        return mockMvc.perform(post("/api/households/{h}/financial-snapshots", householdId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
