package com.waypoint.household;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.HashMap;
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
 * Exercises the asset/liability API against a real PostgreSQL instance so the
 * Task 001 -> Task 002 Flyway upgrade path, JPA mapping, and REST boundary are
 * verified together.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AssetLiabilityApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createsAndRetrievesAsset() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        String body = createAsset(householdId, "Emergency Fund", "CASH", "1000.00", "1000.00", "php",
                LocalDate.now().toString(), "LIQUID")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.householdId").value(householdId))
                .andExpect(jsonPath("$.name").value("Emergency Fund"))
                .andExpect(jsonPath("$.assetType").value("CASH"))
                .andExpect(jsonPath("$.estimatedValue").value(1000.00))
                .andExpect(jsonPath("$.planningValue").value(1000.00))
                .andExpect(jsonPath("$.currency").value("PHP"))
                .andExpect(jsonPath("$.liquidity").value("LIQUID"))
                .andExpect(jsonPath("$.sourceType").value("MANUAL_ENTRY"))
                .andReturn().getResponse().getContentAsString();
        String assetId = objectMapper.readTree(body).get("id").asText();

        mockMvc.perform(get("/api/households/{h}/assets/{a}", householdId, assetId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(assetId))
                .andExpect(jsonPath("$.name").value("Emergency Fund"));
    }

    @Test
    void acceptsZeroAssetValues() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createAsset(householdId, "Empty Wallet", "CASH", "0", "0", "PHP",
                LocalDate.now().toString(), "LIQUID")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estimatedValue").value(0))
                .andExpect(jsonPath("$.planningValue").value(0));
    }

    @Test
    void rejectsNegativeAssetValues() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createAsset(householdId, "Bad Fund", "CASH", "-1", "0", "PHP",
                LocalDate.now().toString(), "LIQUID")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsPlanningValueGreaterThanEstimatedValue() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createAsset(householdId, "Startup Equity", "BUSINESS_OWNERSHIP", "100.00", "150.00", "PHP",
                LocalDate.now().toString(), "ILLIQUID")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsBlankAssetName() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createAsset(householdId, "  ", "CASH", "1", "1", "PHP", LocalDate.now().toString(), "LIQUID")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsMalformedAssetCurrency() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createAsset(householdId, "Fund", "CASH", "1", "1", "PESO", LocalDate.now().toString(), "LIQUID")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsUnrecognizedAssetType() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createAsset(householdId, "Fund", "CRYPTO", "1", "1", "PHP", LocalDate.now().toString(), "LIQUID")
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsFutureValuedAtDate() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createAsset(householdId, "Fund", "CASH", "1", "1", "PHP", LocalDate.now().plusDays(1).toString(), "LIQUID")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsCreatingAssetForUnknownHousehold() throws Exception {
        createAsset(UUID.randomUUID().toString(), "Fund", "CASH", "1", "1", "PHP",
                LocalDate.now().toString(), "LIQUID")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("HOUSEHOLD_NOT_FOUND"));
    }

    @Test
    void returnsNotFoundWhenAssetBelongsToAnotherHousehold() throws Exception {
        String householdOneId = createHouseholdId("Household One", "PHP");
        String householdTwoId = createHouseholdId("Household Two", "PHP");

        String body = createAsset(householdOneId, "Fund", "CASH", "1", "1", "PHP",
                LocalDate.now().toString(), "LIQUID")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String assetId = objectMapper.readTree(body).get("id").asText();

        mockMvc.perform(get("/api/households/{h}/assets/{a}", householdTwoId, assetId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ASSET_NOT_FOUND"));
    }

    @Test
    void newHouseholdHasEmptyAssetList() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        mockMvc.perform(get("/api/households/{h}/assets", householdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listsAssetsInCreationOrderAndAllowsDuplicateNames() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createAsset(householdId, "Cash", "CASH", "1", "1", "PHP", LocalDate.now().toString(), "LIQUID")
                .andExpect(status().isCreated());
        createAsset(householdId, "Cash", "CASH", "2", "2", "PHP", LocalDate.now().toString(), "LIQUID")
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/households/{h}/assets", householdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].estimatedValue").value(1))
                .andExpect(jsonPath("$[1].estimatedValue").value(2));
    }

    @Test
    void createsAndRetrievesLiability() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        String body = createLiability(householdId, "Credit Card", "CREDIT_CARD", "500.00", "PHP",
                LocalDate.now().toString())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.householdId").value(householdId))
                .andExpect(jsonPath("$.name").value("Credit Card"))
                .andExpect(jsonPath("$.liabilityType").value("CREDIT_CARD"))
                .andExpect(jsonPath("$.outstandingBalance").value(500.00))
                .andExpect(jsonPath("$.sourceType").value("MANUAL_ENTRY"))
                .andReturn().getResponse().getContentAsString();
        String liabilityId = objectMapper.readTree(body).get("id").asText();

        mockMvc.perform(get("/api/households/{h}/liabilities/{l}", householdId, liabilityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(liabilityId));
    }

    @Test
    void acceptsZeroLiabilityBalance() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createLiability(householdId, "Paid Off Card", "CREDIT_CARD", "0", "PHP", LocalDate.now().toString())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.outstandingBalance").value(0));
    }

    @Test
    void rejectsNegativeLiabilityBalance() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createLiability(householdId, "Bad Loan", "PERSONAL_LOAN", "-1", "PHP", LocalDate.now().toString())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsCreatingLiabilityForUnknownHousehold() throws Exception {
        createLiability(UUID.randomUUID().toString(), "Loan", "PERSONAL_LOAN", "1", "PHP",
                LocalDate.now().toString())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("HOUSEHOLD_NOT_FOUND"));
    }

    @Test
    void returnsNotFoundWhenLiabilityBelongsToAnotherHousehold() throws Exception {
        String householdOneId = createHouseholdId("Household One", "PHP");
        String householdTwoId = createHouseholdId("Household Two", "PHP");

        String body = createLiability(householdOneId, "Loan", "PERSONAL_LOAN", "1", "PHP",
                LocalDate.now().toString())
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String liabilityId = objectMapper.readTree(body).get("id").asText();

        mockMvc.perform(get("/api/households/{h}/liabilities/{l}", householdTwoId, liabilityId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("LIABILITY_NOT_FOUND"));
    }

    @Test
    void newHouseholdHasEmptyLiabilityList() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        mockMvc.perform(get("/api/households/{h}/liabilities", householdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void keepsAssetsAndLiabilitiesIsolatedBetweenHouseholds() throws Exception {
        String householdOneId = createHouseholdId("Household One", "PHP");
        String householdTwoId = createHouseholdId("Household Two", "PHP");

        createAsset(householdOneId, "Fund A", "CASH", "1", "1", "PHP", LocalDate.now().toString(), "LIQUID")
                .andExpect(status().isCreated());
        createAsset(householdTwoId, "Fund B", "CASH", "2", "2", "PHP", LocalDate.now().toString(), "LIQUID")
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/households/{h}/assets", householdOneId))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Fund A"));
        mockMvc.perform(get("/api/households/{h}/assets", householdTwoId))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Fund B"));
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
            String householdId, String name, String assetType, String estimatedValue, String planningValue,
            String currency, String valuedAt, String liquidity
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
        return mockMvc.perform(post("/api/households/{h}/assets", householdId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions createLiability(
            String householdId, String name, String liabilityType, String outstandingBalance, String currency,
            String balanceAsOf
    ) throws Exception {
        String body = objectMapper.writeValueAsString(new HashMap<>() {{
            put("name", name);
            put("liabilityType", liabilityType);
            put("outstandingBalance", outstandingBalance);
            put("currency", currency);
            put("balanceAsOf", balanceAsOf);
        }});
        return mockMvc.perform(post("/api/households/{h}/liabilities", householdId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
