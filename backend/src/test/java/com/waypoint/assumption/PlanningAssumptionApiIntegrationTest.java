package com.waypoint.assumption;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
 * Exercises the planning assumption API against a real PostgreSQL instance so
 * the Flyway migration, JPA mapping, and REST boundary are verified together.
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
    void createsAndRetrievesAssumptionAsManualEntryWithNoSupersession() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        String body = createAssumption(householdId, "Future monthly income", "150000", "PHP/month",
                "Assumes current raise holds", LocalDate.now().toString(), null,
                LocalDate.now().plusMonths(6).toString())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.householdId").value(householdId))
                .andExpect(jsonPath("$.name").value("Future monthly income"))
                .andExpect(jsonPath("$.value").value("150000"))
                .andExpect(jsonPath("$.valueType").value("PHP/month"))
                .andExpect(jsonPath("$.notes").value("Assumes current raise holds"))
                .andExpect(jsonPath("$.sourceType").value("MANUAL_ENTRY"))
                .andExpect(jsonPath("$.supersededBy").doesNotExist())
                .andExpect(jsonPath("$.createdAt").exists())
                .andReturn().getResponse().getContentAsString();
        String assumptionId = objectMapper.readTree(body).get("id").asText();

        mockMvc.perform(get("/api/households/{h}/assumptions/{a}", householdId, assumptionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(assumptionId))
                .andExpect(jsonPath("$.name").value("Future monthly income"));
    }

    @Test
    void trimsWhitespaceAndBlankNotesBecomeNull() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createAssumption(householdId, "  Future monthly income  ", "  150000  ", "  PHP/month  ", "   ",
                LocalDate.now().toString(), null, LocalDate.now().plusMonths(6).toString())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Future monthly income"))
                .andExpect(jsonPath("$.value").value("150000"))
                .andExpect(jsonPath("$.valueType").value("PHP/month"))
                .andExpect(jsonPath("$.notes").doesNotExist());
    }

    @Test
    void rejectsBlankName() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createAssumption(householdId, "  ", "150000", "PHP/month", null,
                LocalDate.now().toString(), null, LocalDate.now().plusMonths(6).toString())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsOversizedName() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createAssumption(householdId, "x".repeat(256), "150000", "PHP/month", null,
                LocalDate.now().toString(), null, LocalDate.now().plusMonths(6).toString())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsBlankValue() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createAssumption(householdId, "Future monthly income", "  ", "PHP/month", null,
                LocalDate.now().toString(), null, LocalDate.now().plusMonths(6).toString())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsBlankValueType() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createAssumption(householdId, "Future monthly income", "150000", "  ", null,
                LocalDate.now().toString(), null, LocalDate.now().plusMonths(6).toString())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsMissingEffectiveFrom() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createAssumption(householdId, "Future monthly income", "150000", "PHP/month", null,
                null, null, LocalDate.now().plusMonths(6).toString())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsMissingReviewDate() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createAssumption(householdId, "Future monthly income", "150000", "PHP/month", null,
                LocalDate.now().toString(), null, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsEffectiveUntilBeforeEffectiveFrom() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createAssumption(householdId, "Future monthly income", "150000", "PHP/month", null,
                LocalDate.now().toString(), LocalDate.now().minusDays(1).toString(),
                LocalDate.now().plusMonths(6).toString())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void acceptsEffectiveUntilEqualToEffectiveFrom() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createAssumption(householdId, "Future monthly income", "150000", "PHP/month", null,
                LocalDate.now().toString(), LocalDate.now().toString(),
                LocalDate.now().plusMonths(6).toString())
                .andExpect(status().isCreated());
    }

    @Test
    void rejectsCreatingAssumptionForUnknownHousehold() throws Exception {
        createAssumption(UUID.randomUUID().toString(), "Future monthly income", "150000", "PHP/month", null,
                LocalDate.now().toString(), null, LocalDate.now().plusMonths(6).toString())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("HOUSEHOLD_NOT_FOUND"));
    }

    @Test
    void returnsNotFoundWhenGettingForUnknownHousehold() throws Exception {
        mockMvc.perform(get("/api/households/{h}/assumptions/{a}", UUID.randomUUID(), UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("HOUSEHOLD_NOT_FOUND"));
    }

    @Test
    void returnsNotFoundWhenAssumptionBelongsToAnotherHousehold() throws Exception {
        String householdOneId = createHouseholdId("Household One", "PHP");
        String householdTwoId = createHouseholdId("Household Two", "PHP");

        String body = createAssumption(householdOneId, "Future monthly income", "150000", "PHP/month", null,
                LocalDate.now().toString(), null, LocalDate.now().plusMonths(6).toString())
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String assumptionId = objectMapper.readTree(body).get("id").asText();

        mockMvc.perform(get("/api/households/{h}/assumptions/{a}", householdTwoId, assumptionId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("PLANNING_ASSUMPTION_NOT_FOUND"));
    }

    @Test
    void newHouseholdHasEmptyAssumptionList() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        mockMvc.perform(get("/api/households/{h}/assumptions", householdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listsAssumptionsOrderedByNameThenCreationForFullHistory() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createAssumption(householdId, "Tuition inflation", "5%", "annual rate", null,
                LocalDate.now().toString(), null, LocalDate.now().plusMonths(6).toString())
                .andExpect(status().isCreated());
        createAssumption(householdId, "Expected investment return", "7%", "annual rate", null,
                LocalDate.now().toString(), null, LocalDate.now().plusMonths(6).toString())
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/households/{h}/assumptions", householdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Expected investment return"))
                .andExpect(jsonPath("$[1].name").value("Tuition inflation"));
    }

    @Test
    void keepsAssumptionsIsolatedBetweenHouseholds() throws Exception {
        String householdOneId = createHouseholdId("Household One", "PHP");
        String householdTwoId = createHouseholdId("Household Two", "PHP");

        createAssumption(householdOneId, "Assumption A", "1", "unit", null,
                LocalDate.now().toString(), null, LocalDate.now().plusMonths(6).toString())
                .andExpect(status().isCreated());
        createAssumption(householdTwoId, "Assumption B", "2", "unit", null,
                LocalDate.now().toString(), null, LocalDate.now().plusMonths(6).toString())
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/households/{h}/assumptions", householdOneId))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Assumption A"));
        mockMvc.perform(get("/api/households/{h}/assumptions", householdTwoId))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Assumption B"));
    }

    @Test
    void rejectsActiveOnlyListingWithoutAsOf() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        mockMvc.perform(get("/api/households/{h}/assumptions?activeOnly=true", householdId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void activeAsOfExcludesOutOfWindowAssumptionsAndIncludesOpenEndedOnes() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        LocalDate today = LocalDate.now();

        createAssumption(householdId, "Not yet effective", "1", "unit", null,
                today.plusMonths(1).toString(), null, today.plusMonths(6).toString())
                .andExpect(status().isCreated());
        createAssumption(householdId, "Already expired", "2", "unit", null,
                today.minusMonths(2).toString(), today.minusDays(1).toString(), today.plusMonths(6).toString())
                .andExpect(status().isCreated());
        createAssumption(householdId, "Open ended and active", "3", "unit", null,
                today.minusMonths(1).toString(), null, today.plusMonths(6).toString())
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/households/{h}/assumptions?activeOnly=true&asOf={d}", householdId, today))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Open ended and active"));
    }

    @Test
    void activeAsOfDoesNotConsultSystemClockAndUsesSuppliedDateInstead() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        LocalDate today = LocalDate.now();

        createAssumption(householdId, "Future assumption", "1", "unit", null,
                today.plusMonths(1).toString(), null, today.plusMonths(6).toString())
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/households/{h}/assumptions?activeOnly=true&asOf={d}",
                        householdId, today.plusMonths(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Future assumption"));
    }

    @Test
    void supersedeCreatesReplacementAndLeavesPriorRetrievableWithSupersededByLink() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        String priorBody = createAssumption(householdId, "Future monthly income", "150000", "PHP/month", null,
                LocalDate.now().toString(), null, LocalDate.now().plusMonths(6).toString())
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String priorId = objectMapper.readTree(priorBody).get("id").asText();

        String replacementBody = supersedeAssumption(householdId, priorId, "Future monthly income", "160000",
                "PHP/month", "Raise confirmed", LocalDate.now().toString(), null,
                LocalDate.now().plusMonths(6).toString())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.value").value("160000"))
                .andExpect(jsonPath("$.supersededBy").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String replacementId = objectMapper.readTree(replacementBody).get("id").asText();

        mockMvc.perform(get("/api/households/{h}/assumptions/{a}", householdId, priorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value("150000"))
                .andExpect(jsonPath("$.supersededBy").value(replacementId));

        mockMvc.perform(get("/api/households/{h}/assumptions", householdId))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void rejectsSupersedingAnAlreadySupersededAssumption() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        String priorBody = createAssumption(householdId, "Future monthly income", "150000", "PHP/month", null,
                LocalDate.now().toString(), null, LocalDate.now().plusMonths(6).toString())
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String priorId = objectMapper.readTree(priorBody).get("id").asText();

        supersedeAssumption(householdId, priorId, "Future monthly income", "160000", "PHP/month", null,
                LocalDate.now().toString(), null, LocalDate.now().plusMonths(6).toString())
                .andExpect(status().isCreated());

        supersedeAssumption(householdId, priorId, "Future monthly income", "170000", "PHP/month", null,
                LocalDate.now().toString(), null, LocalDate.now().plusMonths(6).toString())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ASSUMPTION_ALREADY_SUPERSEDED"));
    }

    @Test
    void rejectsSupersedingWithMismatchedNameAndChangesNothing() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        String priorBody = createAssumption(householdId, "Future monthly income", "150000", "PHP/month", null,
                LocalDate.now().toString(), null, LocalDate.now().plusMonths(6).toString())
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String priorId = objectMapper.readTree(priorBody).get("id").asText();

        supersedeAssumption(householdId, priorId, "Expected investment return", "160000", "PHP/month", null,
                LocalDate.now().toString(), null, LocalDate.now().plusMonths(6).toString())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));

        mockMvc.perform(get("/api/households/{h}/assumptions/{a}", householdId, priorId))
                .andExpect(jsonPath("$.supersededBy").doesNotExist());
        mockMvc.perform(get("/api/households/{h}/assumptions", householdId))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void rejectsSupersedingAssumptionFromAnotherHousehold() throws Exception {
        String householdOneId = createHouseholdId("Household One", "PHP");
        String householdTwoId = createHouseholdId("Household Two", "PHP");

        String priorBody = createAssumption(householdOneId, "Future monthly income", "150000", "PHP/month", null,
                LocalDate.now().toString(), null, LocalDate.now().plusMonths(6).toString())
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String priorId = objectMapper.readTree(priorBody).get("id").asText();

        supersedeAssumption(householdTwoId, priorId, "Future monthly income", "160000", "PHP/month", null,
                LocalDate.now().toString(), null, LocalDate.now().plusMonths(6).toString())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("PLANNING_ASSUMPTION_NOT_FOUND"));
    }

    @Test
    void rejectsSupersedingWithInvalidReplacementFieldsAndChangesNothing() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        String priorBody = createAssumption(householdId, "Future monthly income", "150000", "PHP/month", null,
                LocalDate.now().toString(), null, LocalDate.now().plusMonths(6).toString())
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String priorId = objectMapper.readTree(priorBody).get("id").asText();

        supersedeAssumption(householdId, priorId, "Future monthly income", "  ", "PHP/month", null,
                LocalDate.now().toString(), null, LocalDate.now().plusMonths(6).toString())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));

        mockMvc.perform(get("/api/households/{h}/assumptions/{a}", householdId, priorId))
                .andExpect(jsonPath("$.value").value("150000"))
                .andExpect(jsonPath("$.supersededBy").doesNotExist());
    }

    @Test
    void hasNoOrdinaryUpdateOrDeleteEndpoint() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        String priorBody = createAssumption(householdId, "Future monthly income", "150000", "PHP/month", null,
                LocalDate.now().toString(), null, LocalDate.now().plusMonths(6).toString())
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String priorId = objectMapper.readTree(priorBody).get("id").asText();

        mockMvc.perform(put("/api/households/{h}/assumptions/{a}", householdId, priorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(delete("/api/households/{h}/assumptions/{a}", householdId, priorId))
                .andExpect(status().isMethodNotAllowed());
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
