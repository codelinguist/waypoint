package com.waypoint.review.freshness.web;

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
 * Exercises the read-only financial-data-freshness endpoint against a real
 * PostgreSQL instance, reusing the existing household/asset/liability HTTP
 * API (no direct repository access) to build synthetic fixtures. Every date
 * fixture is anchored to {@link LocalDate#now()} at test-run time rather
 * than a fixed calendar date, so these tests pass on any run date while
 * still satisfying the existing asset/liability "not in the future"
 * validation.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class FinancialDataFreshnessApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void classifiesCurrentStaleAndFutureDatedRecordsAtTheThreshold() throws Exception {
        String householdId = createHouseholdId();
        LocalDate reviewDate = LocalDate.now().minusDays(10);

        createAsset(householdId, "Exactly at threshold", reviewDate.minusDays(30));
        createLiability(householdId, "One day past threshold", reviewDate.minusDays(31));
        createAsset(householdId, "Dated after review date", reviewDate.plusDays(1));

        review(householdId, reviewDate, 30)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records.length()").value(3))
                .andExpect(jsonPath("$.countsByClassification.CURRENT").value(1))
                .andExpect(jsonPath("$.countsByClassification.STALE").value(1))
                .andExpect(jsonPath("$.countsByClassification.FUTURE_DATED").value(1));

        String body = review(householdId, reviewDate, 30).andReturn().getResponse().getContentAsString();
        var records = objectMapper.readTree(body).get("records");
        boolean sawCurrentAt30 = false;
        boolean sawStaleAt31 = false;
        boolean sawFutureDatedAtMinus1 = false;
        for (var record : records) {
            long ageDays = record.get("ageDays").asLong();
            String classification = record.get("classification").asText();
            if (ageDays == 30 && classification.equals("CURRENT")) {
                sawCurrentAt30 = true;
            }
            if (ageDays == 31 && classification.equals("STALE")) {
                sawStaleAt31 = true;
            }
            if (ageDays == -1 && classification.equals("FUTURE_DATED")) {
                sawFutureDatedAtMinus1 = true;
            }
        }
        org.assertj.core.api.Assertions.assertThat(sawCurrentAt30).isTrue();
        org.assertj.core.api.Assertions.assertThat(sawStaleAt31).isTrue();
        org.assertj.core.api.Assertions.assertThat(sawFutureDatedAtMinus1).isTrue();
    }

    @Test
    void zeroThresholdMarksSameDayCurrentAndEarlierStale() throws Exception {
        String householdId = createHouseholdId();
        LocalDate reviewDate = LocalDate.now().minusDays(5);

        createAsset(householdId, "Same day", reviewDate);
        createAsset(householdId, "One day earlier", reviewDate.minusDays(1));

        review(householdId, reviewDate, 0)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.countsByClassification.CURRENT").value(1))
                .andExpect(jsonPath("$.countsByClassification.STALE").value(1));
    }

    @Test
    void returnsAllRecordsNotOnlyStaleOnes() throws Exception {
        String householdId = createHouseholdId();
        LocalDate reviewDate = LocalDate.now().minusDays(1);

        createAsset(householdId, "Fresh", reviewDate);

        review(householdId, reviewDate, 30)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records.length()").value(1))
                .andExpect(jsonPath("$.records[0].classification").value("CURRENT"));
    }

    @Test
    void doesNotCopyFinancialAmountsIntoTheResponse() throws Exception {
        String householdId = createHouseholdId();
        LocalDate reviewDate = LocalDate.now().minusDays(1);

        createAsset(householdId, "Fund", reviewDate);

        String body = review(householdId, reviewDate, 30)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(body)
                .doesNotContain("estimatedValue")
                .doesNotContain("planningValue")
                .doesNotContain("outstandingBalance");
    }

    @Test
    void includesRecordSourceTypeAndUsesValuedAtNotCreatedOrUpdatedAt() throws Exception {
        String householdId = createHouseholdId();
        LocalDate reviewDate = LocalDate.now().minusDays(1);
        LocalDate valuedAt = reviewDate.minusDays(5);

        createAsset(householdId, "Fund", valuedAt);

        review(householdId, reviewDate, 30)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].sourceType").value("MANUAL_ENTRY"))
                .andExpect(jsonPath("$.records[0].sourceDate").value(valuedAt.toString()))
                .andExpect(jsonPath("$.records[0].ageDays").value(5));
    }

    @Test
    void newHouseholdYieldsEmptyListAndZeroCounts() throws Exception {
        String householdId = createHouseholdId();

        review(householdId, LocalDate.now(), 30)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records").isArray())
                .andExpect(jsonPath("$.records.length()").value(0))
                .andExpect(jsonPath("$.countsByKind.ASSET").value(0))
                .andExpect(jsonPath("$.countsByKind.LIABILITY").value(0))
                .andExpect(jsonPath("$.countsByClassification.CURRENT").value(0))
                .andExpect(jsonPath("$.countsByClassification.STALE").value(0))
                .andExpect(jsonPath("$.countsByClassification.FUTURE_DATED").value(0));
    }

    @Test
    void keepsRecordsIsolatedBetweenHouseholds() throws Exception {
        String householdOneId = createHouseholdId();
        String householdTwoId = createHouseholdId();
        LocalDate reviewDate = LocalDate.now().minusDays(1);

        createAsset(householdOneId, "Household one fund", reviewDate);
        createAsset(householdTwoId, "Household two fund", reviewDate);
        createAsset(householdTwoId, "Household two fund 2", reviewDate);

        review(householdOneId, reviewDate, 30)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records.length()").value(1))
                .andExpect(jsonPath("$.records[0].name").value("Household one fund"));
        review(householdTwoId, reviewDate, 30)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records.length()").value(2));
    }

    @Test
    void producesStableOrderingAndIdenticalResultsAcrossRepeatedCalls() throws Exception {
        String householdId = createHouseholdId();
        LocalDate reviewDate = LocalDate.now().minusDays(1);

        createLiability(householdId, "Loan", reviewDate);
        createAsset(householdId, "Investment", reviewDate);
        createAsset(householdId, "Cash", reviewDate);

        String first = review(householdId, reviewDate, 30).andReturn().getResponse().getContentAsString();
        String second = review(householdId, reviewDate, 30).andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(second).isEqualTo(first);

        var records = objectMapper.readTree(first).get("records");
        org.assertj.core.api.Assertions.assertThat(records.get(0).get("recordKind").asText()).isEqualTo("ASSET");
        org.assertj.core.api.Assertions.assertThat(records.get(1).get("recordKind").asText()).isEqualTo("ASSET");
        org.assertj.core.api.Assertions.assertThat(records.get(2).get("recordKind").asText()).isEqualTo("LIABILITY");
    }

    @Test
    void performsNoMutationOfUnderlyingRecords() throws Exception {
        String householdId = createHouseholdId();
        LocalDate reviewDate = LocalDate.now().minusDays(1);
        createAsset(householdId, "Fund", reviewDate);

        String assetsBefore = mockMvc.perform(get("/api/households/{h}/assets", householdId))
                .andReturn().getResponse().getContentAsString();

        review(householdId, reviewDate, 30).andExpect(status().isOk());
        review(householdId, reviewDate, 0).andExpect(status().isOk());

        String assetsAfter = mockMvc.perform(get("/api/households/{h}/assets", householdId))
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(assetsAfter).isEqualTo(assetsBefore);
    }

    @Test
    void rejectsUnknownHousehold() throws Exception {
        review(UUID.randomUUID().toString(), LocalDate.now(), 30)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("HOUSEHOLD_NOT_FOUND"));
    }

    @Test
    void rejectsMissingReviewDate() throws Exception {
        String householdId = createHouseholdId();

        mockMvc.perform(get("/api/households/{h}/financial-data-freshness", householdId)
                        .param("maxAgeDays", "30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsMissingMaxAgeDays() throws Exception {
        String householdId = createHouseholdId();

        mockMvc.perform(get("/api/households/{h}/financial-data-freshness", householdId)
                        .param("reviewDate", LocalDate.now().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsMalformedReviewDate() throws Exception {
        String householdId = createHouseholdId();

        mockMvc.perform(get("/api/households/{h}/financial-data-freshness", householdId)
                        .param("reviewDate", "not-a-date")
                        .param("maxAgeDays", "30"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsFractionalMaxAgeDays() throws Exception {
        String householdId = createHouseholdId();

        mockMvc.perform(get("/api/households/{h}/financial-data-freshness", householdId)
                        .param("reviewDate", LocalDate.now().toString())
                        .param("maxAgeDays", "30.5"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsNegativeMaxAgeDays() throws Exception {
        String householdId = createHouseholdId();

        review(householdId, LocalDate.now(), -1)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsMaxAgeDaysAboveUpperBound() throws Exception {
        String householdId = createHouseholdId();

        review(householdId, LocalDate.now(), 36501)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsOverflowingMaxAgeDays() throws Exception {
        String householdId = createHouseholdId();

        mockMvc.perform(get("/api/households/{h}/financial-data-freshness", householdId)
                        .param("reviewDate", LocalDate.now().toString())
                        .param("maxAgeDays", "99999999999999999999"))
                .andExpect(status().isBadRequest());
    }

    private ResultActions review(String householdId, LocalDate reviewDate, int maxAgeDays) throws Exception {
        return mockMvc.perform(get("/api/households/{h}/financial-data-freshness", householdId)
                .param("reviewDate", reviewDate.toString())
                .param("maxAgeDays", String.valueOf(maxAgeDays)));
    }

    private String createHouseholdId() throws Exception {
        String body = objectMapper.writeValueAsString(new HashMap<>() {{
            put("name", "Freshness Review Household");
            put("baseCurrency", "PHP");
        }});
        String response = mockMvc.perform(post("/api/households")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private void createAsset(String householdId, String name, LocalDate valuedAt) throws Exception {
        String body = objectMapper.writeValueAsString(new HashMap<>() {{
            put("name", name);
            put("assetType", "CASH");
            put("estimatedValue", "1000.00");
            put("planningValue", "1000.00");
            put("currency", "PHP");
            put("valuedAt", valuedAt.toString());
            put("liquidity", "LIQUID");
        }});
        mockMvc.perform(post("/api/households/{h}/assets", householdId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    private void createLiability(String householdId, String name, LocalDate balanceAsOf) throws Exception {
        String body = objectMapper.writeValueAsString(new HashMap<>() {{
            put("name", name);
            put("liabilityType", "PERSONAL_LOAN");
            put("outstandingBalance", "500.00");
            put("currency", "PHP");
            put("balanceAsOf", balanceAsOf.toString());
        }});
        mockMvc.perform(post("/api/households/{h}/liabilities", householdId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }
}
