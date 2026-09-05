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
 * Exercises the financial goal API against a real PostgreSQL instance so the
 * Flyway migration, JPA mapping, and REST boundary are verified together.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class FinancialGoalApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createsAndRetrievesGoal() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        String body = createGoal(householdId, "Retirement", "1000000.00", "PHP",
                LocalDate.now().plusYears(20).toString(), 1, "50000.00")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.householdId").value(householdId))
                .andExpect(jsonPath("$.name").value("Retirement"))
                .andExpect(jsonPath("$.targetAmount").value(1000000.00))
                .andExpect(jsonPath("$.currency").value("PHP"))
                .andExpect(jsonPath("$.priority").value(1))
                .andExpect(jsonPath("$.currentAmount").value(50000.00))
                .andExpect(jsonPath("$.remainingAmount").value(950000.00))
                .andExpect(jsonPath("$.progressPercentage").value(5.00))
                .andReturn().getResponse().getContentAsString();
        String goalId = objectMapper.readTree(body).get("id").asText();

        mockMvc.perform(get("/api/households/{h}/goals/{g}", householdId, goalId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(goalId))
                .andExpect(jsonPath("$.name").value("Retirement"));
    }

    @Test
    void normalizesLowercaseCurrencyToUppercase() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createGoal(householdId, "Retirement", "1000.00", "php", LocalDate.now().plusYears(1).toString(), 1, "0.00")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currency").value("PHP"));
    }

    @Test
    void boundsProgressAtOneHundredWhenCurrentAmountExceedsTarget() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createGoal(householdId, "Vacation", "1000.00", "PHP", LocalDate.now().plusMonths(3).toString(), 1, "1500.00")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.progressPercentage").value(100.00))
                .andExpect(jsonPath("$.remainingAmount").value(-500.00))
                .andExpect(jsonPath("$.currentAmount").value(1500.00));
    }

    @Test
    void rejectsZeroTargetAmount() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createGoal(householdId, "Vacation", "0", "PHP", LocalDate.now().plusMonths(3).toString(), 1, "0")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsNegativeTargetAmount() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createGoal(householdId, "Vacation", "-1", "PHP", LocalDate.now().plusMonths(3).toString(), 1, "0")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsNegativeCurrentAmount() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createGoal(householdId, "Vacation", "1000.00", "PHP", LocalDate.now().plusMonths(3).toString(), 1, "-1")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsBlankGoalName() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createGoal(householdId, "  ", "1000.00", "PHP", LocalDate.now().plusMonths(3).toString(), 1, "0")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsMalformedCurrency() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createGoal(householdId, "Vacation", "1000.00", "PESO", LocalDate.now().plusMonths(3).toString(), 1, "0")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsPastTargetDate() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createGoal(householdId, "Vacation", "1000.00", "PHP", LocalDate.now().minusDays(1).toString(), 1, "0")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void acceptsTodayAsTargetDate() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createGoal(householdId, "Vacation", "1000.00", "PHP", LocalDate.now().toString(), 1, "0")
                .andExpect(status().isCreated());
    }

    @Test
    void rejectsZeroPriority() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createGoal(householdId, "Vacation", "1000.00", "PHP", LocalDate.now().plusMonths(3).toString(), 0, "0")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsNegativePriority() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createGoal(householdId, "Vacation", "1000.00", "PHP", LocalDate.now().plusMonths(3).toString(), -1, "0")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsGoalValueWithExcessiveFractionalScale() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createGoal(householdId, "Vacation", "100.005", "PHP", LocalDate.now().plusMonths(3).toString(), 1, "0")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsGoalValueWithPrecisionOverflow() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createGoal(householdId, "Vacation", "123456789012345678.00", "PHP",
                LocalDate.now().plusMonths(3).toString(), 1, "0")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsCreatingGoalForUnknownHousehold() throws Exception {
        createGoal(UUID.randomUUID().toString(), "Vacation", "1000.00", "PHP",
                LocalDate.now().plusMonths(3).toString(), 1, "0")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("HOUSEHOLD_NOT_FOUND"));
    }

    @Test
    void returnsNotFoundWhenGettingGoalForUnknownHousehold() throws Exception {
        mockMvc.perform(get("/api/households/{h}/goals/{g}", UUID.randomUUID(), UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("HOUSEHOLD_NOT_FOUND"));
    }

    @Test
    void returnsNotFoundWhenGoalBelongsToAnotherHousehold() throws Exception {
        String householdOneId = createHouseholdId("Household One", "PHP");
        String householdTwoId = createHouseholdId("Household Two", "PHP");

        String body = createGoal(householdOneId, "Vacation", "1000.00", "PHP",
                LocalDate.now().plusMonths(3).toString(), 1, "0")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String goalId = objectMapper.readTree(body).get("id").asText();

        mockMvc.perform(get("/api/households/{h}/goals/{g}", householdTwoId, goalId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("FINANCIAL_GOAL_NOT_FOUND"));
    }

    @Test
    void newHouseholdHasEmptyGoalList() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        mockMvc.perform(get("/api/households/{h}/goals", householdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listsGoalsOrderedByPriorityAscending() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createGoal(householdId, "Low Priority", "1000.00", "PHP", LocalDate.now().plusMonths(3).toString(), 3, "0")
                .andExpect(status().isCreated());
        createGoal(householdId, "High Priority", "1000.00", "PHP", LocalDate.now().plusMonths(3).toString(), 1, "0")
                .andExpect(status().isCreated());
        createGoal(householdId, "Medium Priority", "1000.00", "PHP", LocalDate.now().plusMonths(3).toString(), 2, "0")
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/households/{h}/goals", householdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].name").value("High Priority"))
                .andExpect(jsonPath("$[1].name").value("Medium Priority"))
                .andExpect(jsonPath("$[2].name").value("Low Priority"));
    }

    @Test
    void keepsGoalsIsolatedBetweenHouseholds() throws Exception {
        String householdOneId = createHouseholdId("Household One", "PHP");
        String householdTwoId = createHouseholdId("Household Two", "PHP");

        createGoal(householdOneId, "Goal A", "1000.00", "PHP", LocalDate.now().plusMonths(3).toString(), 1, "0")
                .andExpect(status().isCreated());
        createGoal(householdTwoId, "Goal B", "1000.00", "PHP", LocalDate.now().plusMonths(3).toString(), 1, "0")
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/households/{h}/goals", householdOneId))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Goal A"));
        mockMvc.perform(get("/api/households/{h}/goals", householdTwoId))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Goal B"));
    }

    @Test
    void creatingGoalDoesNotMutateOtherHouseholdRecords() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
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

        createGoal(householdId, "Vacation", "1000.00", "PHP", LocalDate.now().plusMonths(3).toString(), 1, "0")
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/households/{h}/assets", householdId))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].estimatedValue").value(500.00));
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

    private ResultActions createGoal(
            String householdId, String name, String targetAmount, String currency, String targetDate,
            Integer priority, String currentAmount
    ) throws Exception {
        String body = objectMapper.writeValueAsString(new HashMap<>() {{
            put("name", name);
            put("targetAmount", targetAmount);
            put("currency", currency);
            put("targetDate", targetDate);
            put("priority", priority);
            put("currentAmount", currentAmount);
        }});
        return mockMvc.perform(post("/api/households/{h}/goals", householdId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
