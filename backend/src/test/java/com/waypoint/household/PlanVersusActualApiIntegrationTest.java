package com.waypoint.household;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Exercises the plan-versus-actual analysis API against a real PostgreSQL
 * instance so the read-only, no-mutation contract is verified end to end,
 * alongside the household/snapshot ownership and plan-validation rules.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class PlanVersusActualApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void returnsAboveAndBelowPlanVariancesForEveryPlannedCurrency() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        createAsset(householdId, "PHP Cash", "CASH", "120.00", "PHP", LocalDate.now().toString(), "LIQUID");
        createLiability(householdId, "PHP Loan", "PERSONAL_LOAN", "30.00", "PHP", LocalDate.now().toString());
        String snapshotId = snapshotId(createSnapshot(householdId, LocalDate.now().toString())
                .andExpect(status().isCreated()));

        analyze(householdId, snapshotId, List.of(
                        plannedMeasure("PHP", "100.00", "30.00", "70.00"),
                        plannedMeasure("USD", "50.00", "0.00", "50.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.snapshot.id").value(snapshotId))
                .andExpect(jsonPath("$.currencyResults.length()").value(2))
                .andExpect(jsonPath("$.currencyResults[0].currency").value("PHP"))
                .andExpect(jsonPath("$.currencyResults[0].assetTotal.planned").value(100.00))
                .andExpect(jsonPath("$.currencyResults[0].assetTotal.actual").value(120.00))
                .andExpect(jsonPath("$.currencyResults[0].assetTotal.variance").value(20.00))
                .andExpect(jsonPath("$.currencyResults[0].assetTotal.direction").value("ABOVE_PLAN"))
                .andExpect(jsonPath("$.currencyResults[0].liabilityTotal.variance").value(0.00))
                .andExpect(jsonPath("$.currencyResults[0].liabilityTotal.direction").value("ON_PLAN"))
                .andExpect(jsonPath("$.currencyResults[0].netWorth.variance").value(20.00))
                .andExpect(jsonPath("$.currencyResults[1].currency").value("USD"))
                .andExpect(jsonPath("$.currencyResults[1].assetTotal.actual").value(0))
                .andExpect(jsonPath("$.currencyResults[1].assetTotal.variance").value(-50.00))
                .andExpect(jsonPath("$.currencyResults[1].assetTotal.direction").value("BELOW_PLAN"));
    }

    @Test
    void repeatedIdenticalRequestsProduceTheSameResultAndNoMutation() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        createAsset(householdId, "PHP Cash", "CASH", "100.00", "PHP", LocalDate.now().toString(), "LIQUID");
        String snapshotId = snapshotId(createSnapshot(householdId, LocalDate.now().toString())
                .andExpect(status().isCreated()));
        List<Map<String, Object>> plan = List.of(plannedMeasure("PHP", "100.00", "0.00", "100.00"));

        String firstResponse = analyze(householdId, snapshotId, plan)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String secondResponse = analyze(householdId, snapshotId, plan)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(firstResponse).isEqualTo(secondResponse);
        mockMvc.perform(get("/api/households/{h}/financial-snapshots/{s}", householdId, snapshotId))
                .andExpect(jsonPath("$.assetLineItems.length()").value(1))
                .andExpect(jsonPath("$.assetLineItems[0].value").value(100.00));
    }

    @Test
    void rejectsAnalysisForUnknownHousehold() throws Exception {
        analyze(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                        List.of(plannedMeasure("PHP", "1.00", "0.00", "1.00")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("HOUSEHOLD_NOT_FOUND"));
    }

    @Test
    void rejectsAnalysisForUnknownSnapshot() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        analyze(householdId, UUID.randomUUID().toString(), List.of(plannedMeasure("PHP", "1.00", "0.00", "1.00")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("FINANCIAL_SNAPSHOT_NOT_FOUND"));
    }

    @Test
    void rejectsCrossHouseholdSnapshotOwnership() throws Exception {
        String householdOneId = createHouseholdId("Household One", "PHP");
        String householdTwoId = createHouseholdId("Household Two", "PHP");
        String snapshotId = snapshotId(createSnapshot(householdOneId, LocalDate.now().toString())
                .andExpect(status().isCreated()));

        analyze(householdTwoId, snapshotId, List.of(plannedMeasure("PHP", "1.00", "0.00", "1.00")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("FINANCIAL_SNAPSHOT_NOT_FOUND"));
    }

    @Test
    void rejectsEmptyPlannedMeasures() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        String snapshotId = snapshotId(createSnapshot(householdId, LocalDate.now().toString())
                .andExpect(status().isCreated()));

        mockMvc.perform(post("/api/households/{h}/financial-snapshots/{s}/plan-comparison", householdId, snapshotId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plannedMeasures\": []}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsDuplicatePlannedCurrencies() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        String snapshotId = snapshotId(createSnapshot(householdId, LocalDate.now().toString())
                .andExpect(status().isCreated()));

        analyze(householdId, snapshotId, List.of(
                        plannedMeasure("PHP", "100.00", "0.00", "100.00"),
                        plannedMeasure("PHP", "50.00", "0.00", "50.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsNegativePlannedTotals() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        String snapshotId = snapshotId(createSnapshot(householdId, LocalDate.now().toString())
                .andExpect(status().isCreated()));

        analyze(householdId, snapshotId, List.of(plannedMeasure("PHP", "-1.00", "0.00", "-1.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsInconsistentPlannedNetWorth() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        String snapshotId = snapshotId(createSnapshot(householdId, LocalDate.now().toString())
                .andExpect(status().isCreated()));

        analyze(householdId, snapshotId, List.of(plannedMeasure("PHP", "100.00", "30.00", "50.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsMissingPlannedMeasureFields() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        String snapshotId = snapshotId(createSnapshot(householdId, LocalDate.now().toString())
                .andExpect(status().isCreated()));
        String body = """
                { "plannedMeasures": [ { "currency": "PHP", "assetTotal": "100.00" } ] }
                """;

        mockMvc.perform(post("/api/households/{h}/financial-snapshots/{s}/plan-comparison", householdId, snapshotId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
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

    private String snapshotId(ResultActions createResult) throws Exception {
        String body = createResult.andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private Map<String, Object> plannedMeasure(
            String currency, String assetTotal, String liabilityTotal, String netWorth
    ) {
        Map<String, Object> measure = new HashMap<>();
        measure.put("currency", currency);
        measure.put("assetTotal", assetTotal);
        measure.put("liabilityTotal", liabilityTotal);
        measure.put("netWorth", netWorth);
        return measure;
    }

    private ResultActions analyze(
            String householdId, String snapshotId, List<Map<String, Object>> plannedMeasures
    ) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("plannedMeasures", plannedMeasures);
        String body = objectMapper.writeValueAsString(payload);
        return mockMvc.perform(post("/api/households/{h}/financial-snapshots/{s}/plan-comparison",
                        householdId, snapshotId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
