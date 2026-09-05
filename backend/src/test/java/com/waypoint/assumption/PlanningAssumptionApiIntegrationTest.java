package com.waypoint.assumption;

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
 * Exercises the planning assumption API against a real PostgreSQL instance
 * so the Flyway migration, JPA mapping, and REST boundary are verified
 * together. Uses only synthetic household data created within each test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class PlanningAssumptionApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createsAndRetrievesAssumption() throws Exception {
        String householdId = createHouseholdId();

        String body = createAssumption(householdId, "Expected annual return", "0.06", "PERCENTAGE_ANNUAL",
                "Conservative estimate", LocalDate.of(2026, 1, 1).toString(), null,
                LocalDate.of(2027, 1, 1).toString())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.householdId").value(householdId))
                .andExpect(jsonPath("$.name").value("Expected annual return"))
                .andExpect(jsonPath("$.value").value("0.06"))
                .andExpect(jsonPath("$.valueType").value("PERCENTAGE_ANNUAL"))
                .andExpect(jsonPath("$.notes").value("Conservative estimate"))
                .andExpect(jsonPath("$.effectiveFrom").value("2026-01-01"))
                .andExpect(jsonPath("$.effectiveUntil").doesNotExist())
                .andExpect(jsonPath("$.reviewDate").value("2027-01-01"))
                .andExpect(jsonPath("$.sourceType").value("MANUAL_ENTRY"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.superseded").value(false))
                .andExpect(jsonPath("$.supersededById").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String assumptionId = objectMapper.readTree(body).get("id").asText();

        mockMvc.perform(get("/api/households/{h}/assumptions/{a}", householdId, assumptionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(assumptionId))
                .andExpect(jsonPath("$.name").value("Expected annual return"));
    }

    @Test
    void rejectsBlankName() throws Exception {
        String householdId = createHouseholdId();

        createAssumption(householdId, "  ", "0.06", "PERCENTAGE_ANNUAL", null,
                LocalDate.now().toString(), null, LocalDate.now().plusYears(1).toString())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsBlankValue() throws Exception {
        String householdId = createHouseholdId();

        createAssumption(householdId, "Expected return", "  ", "PERCENTAGE_ANNUAL", null,
                LocalDate.now().toString(), null, LocalDate.now().plusYears(1).toString())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsBlankValueType() throws Exception {
        String householdId = createHouseholdId();

        createAssumption(householdId, "Expected return", "0.06", "  ", null,
                LocalDate.now().toString(), null, LocalDate.now().plusYears(1).toString())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsOversizedName() throws Exception {
        String householdId = createHouseholdId();

        createAssumption(householdId, "x".repeat(256), "0.06", "PERCENTAGE_ANNUAL", null,
                LocalDate.now().toString(), null, LocalDate.now().plusYears(1).toString())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsMissingEffectiveFrom() throws Exception {
        String householdId = createHouseholdId();

        String requestBody = objectMapper.writeValueAsString(new HashMap<>() {{
            put("name", "Expected return");
            put("value", "0.06");
            put("valueType", "PERCENTAGE_ANNUAL");
            put("reviewDate", LocalDate.now().plusYears(1).toString());
        }});
        mockMvc.perform(post("/api/households/{h}/assumptions", householdId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsMissingReviewDate() throws Exception {
        String householdId = createHouseholdId();

        String requestBody = objectMapper.writeValueAsString(new HashMap<>() {{
            put("name", "Expected return");
            put("value", "0.06");
            put("valueType", "PERCENTAGE_ANNUAL");
            put("effectiveFrom", LocalDate.now().toString());
        }});
        mockMvc.perform(post("/api/households/{h}/assumptions", householdId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsEffectiveUntilBeforeEffectiveFromAndPersistsNothing() throws Exception {
        String householdId = createHouseholdId();

        createAssumption(householdId, "Expected return", "0.06", "PERCENTAGE_ANNUAL", null,
                LocalDate.of(2026, 6, 1).toString(), LocalDate.of(2026, 1, 1).toString(),
                LocalDate.of(2027, 1, 1).toString())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));

        mockMvc.perform(get("/api/households/{h}/assumptions", householdId))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void rejectsCreatingAssumptionForUnknownHousehold() throws Exception {
        createAssumption(UUID.randomUUID().toString(), "Expected return", "0.06", "PERCENTAGE_ANNUAL", null,
                LocalDate.now().toString(), null, LocalDate.now().plusYears(1).toString())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("HOUSEHOLD_NOT_FOUND"));
    }

    @Test
    void returnsNotFoundWhenGettingAssumptionForUnknownHousehold() throws Exception {
        mockMvc.perform(get("/api/households/{h}/assumptions/{a}", UUID.randomUUID(), UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("HOUSEHOLD_NOT_FOUND"));
    }

    @Test
    void returnsNotFoundWhenAssumptionBelongsToAnotherHousehold() throws Exception {
        String householdOneId = createHouseholdId();
        String householdTwoId = createHouseholdId();

        String body = createAssumption(householdOneId, "Expected return", "0.06", "PERCENTAGE_ANNUAL", null,
                LocalDate.now().toString(), null, LocalDate.now().plusYears(1).toString())
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String assumptionId = objectMapper.readTree(body).get("id").asText();

        mockMvc.perform(get("/api/households/{h}/assumptions/{a}", householdTwoId, assumptionId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("PLANNING_ASSUMPTION_NOT_FOUND"));
    }

    @Test
    void newHouseholdHasEmptyAssumptionList() throws Exception {
        String householdId = createHouseholdId();

        mockMvc.perform(get("/api/households/{h}/assumptions", householdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listsAssumptionsOrderedDeterministicallyByName() throws Exception {
        String householdId = createHouseholdId();

        createAssumption(householdId, "Tuition inflation", "0.05", "PERCENTAGE_ANNUAL", null,
                LocalDate.now().toString(), null, LocalDate.now().plusYears(1).toString())
                .andExpect(status().isCreated());
        createAssumption(householdId, "Expected annual return", "0.06", "PERCENTAGE_ANNUAL", null,
                LocalDate.now().toString(), null, LocalDate.now().plusYears(1).toString())
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/households/{h}/assumptions", householdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Expected annual return"))
                .andExpect(jsonPath("$[1].name").value("Tuition inflation"));
    }

    @Test
    void keepsAssumptionsIsolatedBetweenHouseholds() throws Exception {
        String householdOneId = createHouseholdId();
        String householdTwoId = createHouseholdId();

        createAssumption(householdOneId, "Assumption A", "0.06", "PERCENTAGE_ANNUAL", null,
                LocalDate.now().toString(), null, LocalDate.now().plusYears(1).toString())
                .andExpect(status().isCreated());
        createAssumption(householdTwoId, "Assumption B", "0.07", "PERCENTAGE_ANNUAL", null,
                LocalDate.now().toString(), null, LocalDate.now().plusYears(1).toString())
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/households/{h}/assumptions", householdOneId))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Assumption A"));
        mockMvc.perform(get("/api/households/{h}/assumptions", householdTwoId))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Assumption B"));
    }

    @Test
    void activeOnlyRequiresAsOfParameter() throws Exception {
        String householdId = createHouseholdId();

        mockMvc.perform(get("/api/households/{h}/assumptions", householdId).param("activeOnly", "true"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void activeOnlyFilterExcludesOutOfWindowAndIncludesOpenEndedVersions() throws Exception {
        String householdId = createHouseholdId();

        createAssumption(householdId, "Future assumption", "0.05", "PERCENTAGE_ANNUAL", null,
                LocalDate.of(2030, 1, 1).toString(), null, LocalDate.of(2031, 1, 1).toString())
                .andExpect(status().isCreated());
        createAssumption(householdId, "Open ended assumption", "0.06", "PERCENTAGE_ANNUAL", null,
                LocalDate.of(2020, 1, 1).toString(), null, LocalDate.of(2027, 1, 1).toString())
                .andExpect(status().isCreated());
        createAssumption(householdId, "Bounded window assumption", "0.04", "PERCENTAGE_ANNUAL", null,
                LocalDate.of(2020, 1, 1).toString(), LocalDate.of(2021, 12, 31).toString(),
                LocalDate.of(2022, 1, 1).toString())
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/households/{h}/assumptions", householdId)
                        .param("activeOnly", "true")
                        .param("asOf", "2026-06-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Open ended assumption"));
    }

    @Test
    void activeOnlyFilterExcludesSupersededVersions() throws Exception {
        String householdId = createHouseholdId();

        String body = createAssumption(householdId, "Expected return", "0.06", "PERCENTAGE_ANNUAL", null,
                LocalDate.of(2026, 1, 1).toString(), null, LocalDate.of(2027, 1, 1).toString())
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String assumptionId = objectMapper.readTree(body).get("id").asText();

        supersedeAssumption(householdId, assumptionId, "Expected return", "0.07", "PERCENTAGE_ANNUAL", null,
                LocalDate.of(2026, 6, 1).toString(), null, LocalDate.of(2027, 6, 1).toString())
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/households/{h}/assumptions", householdId)
                        .param("activeOnly", "true")
                        .param("asOf", "2026-06-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].value").value("0.07"));
    }

    @Test
    void supersedeCreatesReplacementAndLinksPriorVersionAtomically() throws Exception {
        String householdId = createHouseholdId();

        String body = createAssumption(householdId, "Expected return", "0.06", "PERCENTAGE_ANNUAL", null,
                LocalDate.of(2026, 1, 1).toString(), null, LocalDate.of(2027, 1, 1).toString())
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String priorId = objectMapper.readTree(body).get("id").asText();

        String replacementBody = supersedeAssumption(householdId, priorId, "Expected return", "0.07",
                "PERCENTAGE_ANNUAL", "revised outlook", LocalDate.of(2027, 1, 1).toString(), null,
                LocalDate.of(2028, 1, 1).toString())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.value").value("0.07"))
                .andExpect(jsonPath("$.superseded").value(false))
                .andReturn().getResponse().getContentAsString();
        String replacementId = objectMapper.readTree(replacementBody).get("id").asText();

        mockMvc.perform(get("/api/households/{h}/assumptions/{a}", householdId, priorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value("0.06"))
                .andExpect(jsonPath("$.superseded").value(true))
                .andExpect(jsonPath("$.supersededById").value(replacementId));

        mockMvc.perform(get("/api/households/{h}/assumptions", householdId))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void rejectsSupersedingWithMismatchedNameAndChangesNothing() throws Exception {
        String householdId = createHouseholdId();

        String body = createAssumption(householdId, "Expected return", "0.06", "PERCENTAGE_ANNUAL", null,
                LocalDate.of(2026, 1, 1).toString(), null, LocalDate.of(2027, 1, 1).toString())
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String priorId = objectMapper.readTree(body).get("id").asText();

        supersedeAssumption(householdId, priorId, "A completely different assumption", "0.07",
                "PERCENTAGE_ANNUAL", null, LocalDate.of(2027, 1, 1).toString(), null,
                LocalDate.of(2028, 1, 1).toString())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));

        mockMvc.perform(get("/api/households/{h}/assumptions/{a}", householdId, priorId))
                .andExpect(jsonPath("$.superseded").value(false));
        mockMvc.perform(get("/api/households/{h}/assumptions", householdId))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void rejectsSupersedingAnAlreadySupersededAssumptionAndChangesNothing() throws Exception {
        String householdId = createHouseholdId();

        String body = createAssumption(householdId, "Expected return", "0.06", "PERCENTAGE_ANNUAL", null,
                LocalDate.of(2026, 1, 1).toString(), null, LocalDate.of(2027, 1, 1).toString())
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String priorId = objectMapper.readTree(body).get("id").asText();
        supersedeAssumption(householdId, priorId, "Expected return", "0.07", "PERCENTAGE_ANNUAL", null,
                LocalDate.of(2027, 1, 1).toString(), null, LocalDate.of(2028, 1, 1).toString())
                .andExpect(status().isCreated());

        supersedeAssumption(householdId, priorId, "Expected return", "0.08", "PERCENTAGE_ANNUAL", null,
                LocalDate.of(2027, 1, 1).toString(), null, LocalDate.of(2028, 1, 1).toString())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));

        mockMvc.perform(get("/api/households/{h}/assumptions", householdId))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void returnsNotFoundWhenSupersedingAcrossHouseholds() throws Exception {
        String householdOneId = createHouseholdId();
        String householdTwoId = createHouseholdId();

        String body = createAssumption(householdOneId, "Expected return", "0.06", "PERCENTAGE_ANNUAL", null,
                LocalDate.of(2026, 1, 1).toString(), null, LocalDate.of(2027, 1, 1).toString())
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String assumptionId = objectMapper.readTree(body).get("id").asText();

        supersedeAssumption(householdTwoId, assumptionId, "Expected return", "0.07", "PERCENTAGE_ANNUAL", null,
                LocalDate.of(2027, 1, 1).toString(), null, LocalDate.of(2028, 1, 1).toString())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("PLANNING_ASSUMPTION_NOT_FOUND"));
    }

    @Test
    void creatingAssumptionDoesNotMutateOtherHouseholdRecords() throws Exception {
        String householdId = createHouseholdId();
        String assetBody = objectMapper.writeValueAsString(new HashMap<>() {{
            put("name", "Cash");
            put("assetType", "CASH");
            put("estimatedValue", "500.00");
            put("planningValue", "500.00");
            put("currency", "PHP");
            put("valuedAt", LocalDate.now().toString());
            put("liquidity", "LIQUID");
        }});
        mockMvc.perform(post("/api/households/{h}/assets", householdId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assetBody))
                .andExpect(status().isCreated());

        createAssumption(householdId, "Expected return", "0.06", "PERCENTAGE_ANNUAL", null,
                LocalDate.now().toString(), null, LocalDate.now().plusYears(1).toString())
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/households/{h}/assets", householdId))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].estimatedValue").value(500.00));
    }

    private String createHouseholdId() throws Exception {
        String body = objectMapper.writeValueAsString(new HashMap<>() {{
            put("name", "Ralph Household");
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
            String householdId, String name, String value, String valueType, String notes,
            String effectiveFrom, String effectiveUntil, String reviewDate
    ) throws Exception {
        String body = objectMapper.writeValueAsString(new HashMap<>() {{
            put("name", name);
            put("value", value);
            put("valueType", valueType);
            put("notes", notes);
            put("effectiveFrom", effectiveFrom);
            put("effectiveUntil", effectiveUntil);
            put("reviewDate", reviewDate);
        }});
        return mockMvc.perform(post("/api/households/{h}/assumptions", householdId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions supersedeAssumption(
            String householdId, String assumptionId, String name, String value, String valueType, String notes,
            String effectiveFrom, String effectiveUntil, String reviewDate
    ) throws Exception {
        String body = objectMapper.writeValueAsString(new HashMap<>() {{
            put("name", name);
            put("value", value);
            put("valueType", valueType);
            put("notes", notes);
            put("effectiveFrom", effectiveFrom);
            put("effectiveUntil", effectiveUntil);
            put("reviewDate", reviewDate);
        }});
        return mockMvc.perform(post("/api/households/{h}/assumptions/{a}/supersede", householdId, assumptionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
