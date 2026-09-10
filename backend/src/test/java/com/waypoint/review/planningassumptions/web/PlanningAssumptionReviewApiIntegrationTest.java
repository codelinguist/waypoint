package com.waypoint.review.planningassumptions.web;

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
 * Exercises the read-only planning-assumption review endpoint against a real
 * PostgreSQL instance, reusing the existing household/assumption HTTP API
 * (no direct repository access) to build synthetic fixtures.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class PlanningAssumptionReviewApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void classifiesReviewDatesAtTheAcceptanceCriteriaBoundary() throws Exception {
        String householdId = createHouseholdId();
        LocalDate asOf = LocalDate.of(2026, 9, 10);

        createAssumption(householdId, "Overdue review", LocalDate.of(2026, 1, 1), null,
                LocalDate.of(2026, 9, 9));
        createAssumption(householdId, "Due today", LocalDate.of(2026, 1, 1), null,
                LocalDate.of(2026, 9, 10));
        createAssumption(householdId, "Upcoming review", LocalDate.of(2026, 1, 1), null,
                LocalDate.of(2026, 9, 11));

        review(householdId, asOf)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(3))
                .andExpect(jsonPath("$.countsByReviewStatus.OVERDUE").value(1))
                .andExpect(jsonPath("$.countsByReviewStatus.DUE_TODAY").value(1))
                .andExpect(jsonPath("$.countsByReviewStatus.UPCOMING").value(1));
    }

    @Test
    void effectiveUntilEqualToAsOfIsEffectiveAndTheNextDayIsExpired() throws Exception {
        String householdId = createHouseholdId();
        LocalDate effectiveUntil = LocalDate.of(2026, 6, 30);

        createAssumption(householdId, "Ends June 30", LocalDate.of(2026, 1, 1), effectiveUntil,
                LocalDate.of(2026, 12, 1));

        review(householdId, effectiveUntil)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].effectiveStatus").value("EFFECTIVE"));
        review(householdId, effectiveUntil.plusDays(1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].effectiveStatus").value("EXPIRED"));
    }

    @Test
    void nullEffectiveUntilNeverExpiresAndFutureStartIsNotYetEffective() throws Exception {
        String householdId = createHouseholdId();

        createAssumption(householdId, "Open ended", LocalDate.of(2020, 1, 1), null, LocalDate.of(2026, 1, 1));
        createAssumption(householdId, "Future start", LocalDate.of(2027, 1, 1), null, LocalDate.of(2026, 1, 1));

        String body = review(householdId, LocalDate.of(2026, 9, 10))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        var rows = objectMapper.readTree(body).get("rows");
        boolean sawOpenEndedEffective = false;
        boolean sawFutureStartNotYetEffective = false;
        for (var row : rows) {
            if (row.get("name").asText().equals("Open ended")
                    && row.get("effectiveStatus").asText().equals("EFFECTIVE")) {
                sawOpenEndedEffective = true;
            }
            if (row.get("name").asText().equals("Future start")
                    && row.get("effectiveStatus").asText().equals("NOT_YET_EFFECTIVE")) {
                sawFutureStartNotYetEffective = true;
            }
        }
        org.assertj.core.api.Assertions.assertThat(sawOpenEndedEffective).isTrue();
        org.assertj.core.api.Assertions.assertThat(sawFutureStartNotYetEffective).isTrue();
    }

    @Test
    void expiredAssumptionWithUpcomingReviewStillNeedsAttentionAndFutureEffectiveOverdueRetainsDueStatus()
            throws Exception {
        String householdId = createHouseholdId();
        LocalDate asOf = LocalDate.of(2026, 9, 10);

        createAssumption(householdId, "Expired, not due", LocalDate.of(2020, 1, 1),
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 1));
        createAssumption(householdId, "Not started, overdue", LocalDate.of(2026, 12, 1), null,
                LocalDate.of(2026, 9, 1));

        String body = review(householdId, asOf)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.needsAttentionCount").value(2))
                .andReturn().getResponse().getContentAsString();
        var rows = objectMapper.readTree(body).get("rows");
        for (var row : rows) {
            org.assertj.core.api.Assertions.assertThat(row.get("needsAttention").asBoolean()).isTrue();
            if (row.get("name").asText().equals("Expired, not due")) {
                org.assertj.core.api.Assertions.assertThat(row.get("reviewStatus").asText()).isEqualTo("UPCOMING");
                org.assertj.core.api.Assertions.assertThat(row.get("effectiveStatus").asText()).isEqualTo("EXPIRED");
            }
            if (row.get("name").asText().equals("Not started, overdue")) {
                org.assertj.core.api.Assertions.assertThat(row.get("reviewStatus").asText()).isEqualTo("OVERDUE");
                org.assertj.core.api.Assertions.assertThat(row.get("effectiveStatus").asText())
                        .isEqualTo("NOT_YET_EFFECTIVE");
            }
        }
    }

    @Test
    void excludesSupersededVersionsRegardlessOfTheirDatesAndIncludesUnsupersededFutureAndExpiredRecords()
            throws Exception {
        String householdId = createHouseholdId();
        LocalDate asOf = LocalDate.of(2026, 9, 10);

        String priorBody = createAssumption(householdId, "Will be superseded", LocalDate.of(2020, 1, 1), null,
                LocalDate.of(2026, 1, 1))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String priorId = objectMapper.readTree(priorBody).get("id").asText();
        supersedeAssumption(householdId, priorId, "Will be superseded", LocalDate.of(2020, 1, 1), null,
                LocalDate.of(2026, 1, 1))
                .andExpect(status().isCreated());

        review(householdId, asOf)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.rows[0].name").value("Will be superseded"));
    }

    @Test
    void countsAndNeedsAttentionAreDerivedFromExactlyTheReturnedRows() throws Exception {
        String householdId = createHouseholdId();
        LocalDate asOf = LocalDate.of(2026, 9, 10);

        createAssumption(householdId, "Overdue", LocalDate.of(2026, 1, 1), null, LocalDate.of(2026, 9, 1));
        createAssumption(householdId, "Upcoming", LocalDate.of(2026, 1, 1), null, LocalDate.of(2026, 12, 1));

        review(householdId, asOf)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.needsAttentionCount").value(1))
                .andExpect(jsonPath("$.countsByReviewStatus.OVERDUE").value(1))
                .andExpect(jsonPath("$.countsByReviewStatus.UPCOMING").value(1))
                .andExpect(jsonPath("$.countsByEffectiveStatus.EFFECTIVE").value(2));
    }

    @Test
    void newHouseholdYieldsEmptyRowsAndZeroCounts() throws Exception {
        String householdId = createHouseholdId();

        review(householdId, LocalDate.of(2026, 9, 10))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows").isArray())
                .andExpect(jsonPath("$.rows.length()").value(0))
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.needsAttentionCount").value(0))
                .andExpect(jsonPath("$.countsByReviewStatus.OVERDUE").value(0))
                .andExpect(jsonPath("$.countsByReviewStatus.DUE_TODAY").value(0))
                .andExpect(jsonPath("$.countsByReviewStatus.UPCOMING").value(0))
                .andExpect(jsonPath("$.countsByEffectiveStatus.NOT_YET_EFFECTIVE").value(0))
                .andExpect(jsonPath("$.countsByEffectiveStatus.EFFECTIVE").value(0))
                .andExpect(jsonPath("$.countsByEffectiveStatus.EXPIRED").value(0));
    }

    @Test
    void ordersRowsByReviewDateThenId() throws Exception {
        String householdId = createHouseholdId();

        createAssumption(householdId, "Later", LocalDate.of(2026, 1, 1), null, LocalDate.of(2026, 10, 1));
        createAssumption(householdId, "Earlier", LocalDate.of(2026, 1, 1), null, LocalDate.of(2026, 9, 1));

        String first = review(householdId, LocalDate.of(2026, 9, 10)).andReturn().getResponse().getContentAsString();
        String second = review(householdId, LocalDate.of(2026, 9, 10)).andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(second).isEqualTo(first);
        var rows = objectMapper.readTree(first).get("rows");
        org.assertj.core.api.Assertions.assertThat(rows.get(0).get("name").asText()).isEqualTo("Earlier");
        org.assertj.core.api.Assertions.assertThat(rows.get(1).get("name").asText()).isEqualTo("Later");
    }

    @Test
    void keepsRowsIsolatedBetweenHouseholds() throws Exception {
        String householdOneId = createHouseholdId();
        String householdTwoId = createHouseholdId();
        LocalDate asOf = LocalDate.of(2026, 9, 10);

        createAssumption(householdOneId, "Household one assumption", LocalDate.of(2026, 1, 1), null,
                LocalDate.of(2026, 9, 1));
        createAssumption(householdTwoId, "Household two assumption A", LocalDate.of(2026, 1, 1), null,
                LocalDate.of(2026, 9, 1));
        createAssumption(householdTwoId, "Household two assumption B", LocalDate.of(2026, 1, 1), null,
                LocalDate.of(2026, 9, 1));

        review(householdOneId, asOf)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.rows[0].name").value("Household one assumption"));
        review(householdTwoId, asOf)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(2));
    }

    @Test
    void excludesAssumptionValueAndNotesFromTheResponse() throws Exception {
        String householdId = createHouseholdId();

        createAssumption(householdId, "Sensitive", LocalDate.of(2026, 1, 1), null, LocalDate.of(2026, 9, 1));

        String body = review(householdId, LocalDate.of(2026, 9, 10))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(body).doesNotContain("\"value\"").doesNotContain("\"notes\"");
    }

    @Test
    void performsNoMutationOfUnderlyingAssumptionRecords() throws Exception {
        String householdId = createHouseholdId();
        createAssumption(householdId, "Untouched", LocalDate.of(2026, 1, 1), null, LocalDate.of(2026, 9, 1));

        String before = mockMvc.perform(get("/api/households/{h}/assumptions", householdId))
                .andReturn().getResponse().getContentAsString();

        review(householdId, LocalDate.of(2026, 9, 10)).andExpect(status().isOk());
        review(householdId, LocalDate.of(2026, 12, 1)).andExpect(status().isOk());

        String after = mockMvc.perform(get("/api/households/{h}/assumptions", householdId))
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(after).isEqualTo(before);
    }

    @Test
    void rejectsUnknownHousehold() throws Exception {
        review(UUID.randomUUID().toString(), LocalDate.of(2026, 9, 10))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("HOUSEHOLD_NOT_FOUND"));
    }

    @Test
    void rejectsMissingAsOf() throws Exception {
        String householdId = createHouseholdId();

        mockMvc.perform(get("/api/households/{h}/planning-assumptions/review", householdId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsMalformedAsOf() throws Exception {
        String householdId = createHouseholdId();

        mockMvc.perform(get("/api/households/{h}/planning-assumptions/review", householdId)
                        .param("asOf", "not-a-date"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void doesNotConflictWithTheExistingAssumptionByIdRoute() throws Exception {
        String householdId = createHouseholdId();

        // "review" must never be interpreted as a UUID path variable on the
        // sibling /assumptions/{assumptionId} route.
        review(householdId, LocalDate.of(2026, 9, 10)).andExpect(status().isOk());
    }

    private ResultActions review(String householdId, LocalDate asOf) throws Exception {
        return mockMvc.perform(get("/api/households/{h}/planning-assumptions/review", householdId)
                .param("asOf", asOf.toString()));
    }

    private String createHouseholdId() throws Exception {
        String body = objectMapper.writeValueAsString(new HashMap<>() {{
            put("name", "Planning Assumption Review Household");
            put("baseCurrency", "PHP");
        }});
        String response = mockMvc.perform(post("/api/households")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private ResultActions createAssumption(
            String householdId, String name, LocalDate effectiveFrom, LocalDate effectiveUntil, LocalDate reviewDate
    ) throws Exception {
        String body = objectMapper.writeValueAsString(new HashMap<>() {{
            put("name", name);
            put("value", "1");
            put("valueType", "unit");
            put("effectiveFrom", effectiveFrom.toString());
            put("effectiveUntil", effectiveUntil == null ? null : effectiveUntil.toString());
            put("reviewDate", reviewDate.toString());
        }});
        return mockMvc.perform(post("/api/households/{h}/assumptions", householdId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions supersedeAssumption(
            String householdId, String assumptionId, String name, LocalDate effectiveFrom,
            LocalDate effectiveUntil, LocalDate reviewDate
    ) throws Exception {
        String body = objectMapper.writeValueAsString(new HashMap<>() {{
            put("name", name);
            put("value", "2");
            put("valueType", "unit");
            put("effectiveFrom", effectiveFrom.toString());
            put("effectiveUntil", effectiveUntil == null ? null : effectiveUntil.toString());
            put("reviewDate", reviewDate.toString());
        }});
        return mockMvc.perform(post("/api/households/{h}/assumptions/{a}/supersede", householdId, assumptionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
