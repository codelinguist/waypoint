package com.waypoint.planning.multigoalfunding;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.waypoint.planning.goalcontribution.GoalContributionCalculator;
import com.waypoint.planning.multigoalfunding.web.MultiGoalFundingCheckController;
import com.waypoint.planning.multigoalfunding.web.MultiGoalFundingCheckExceptionHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Exercises {@code POST /api/planning/multi-goal-funding-check} through the
 * real MVC stack, including request validation and the feature-local
 * exception handler. This endpoint has no persistence dependency, so the
 * slice is intentionally isolated with {@link WebMvcTest} instead of a full
 * Spring context or a Postgres/Testcontainers boundary.
 */
@WebMvcTest(controllers = MultiGoalFundingCheckController.class)
@Import({MultiGoalFundingCheckCalculator.class, GoalContributionCalculator.class, MultiGoalFundingCheckExceptionHandler.class})
class MultiGoalFundingCheckApiIntegrationTest {

    private static final String ENDPOINT = "/api/planning/multi-goal-funding-check";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void returnsShortfallForTheDocumentedTwoGoalExample() throws Exception {
        Map<String, Object> payload = request("PHP", "120", List.of(
                goal("education", "100", "0", 3),
                goal("travel", "200", "0", 2)));

        calculate(payload)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("PHP"))
                .andExpect(jsonPath("$.goalResults[0].reference").value("education"))
                .andExpect(jsonPath("$.goalResults[0].monthlyContribution").value(33.34))
                .andExpect(jsonPath("$.goalResults[1].reference").value("travel"))
                .andExpect(jsonPath("$.goalResults[1].monthlyContribution").value(100.00))
                .andExpect(jsonPath("$.totalRequiredMonthlyContribution").value(133.34))
                .andExpect(jsonPath("$.budgetMinusRequired").value(-13.34))
                .andExpect(jsonPath("$.shortfall").value(13.34))
                .andExpect(jsonPath("$.unallocatedBudget").value(0))
                .andExpect(jsonPath("$.status").value("SHORTFALL"));
    }

    @Test
    void returnsFitsWhenBudgetExactlyEqualsTheTotal() throws Exception {
        Map<String, Object> payload = request("PHP", "100", List.of(goal("g1", "300", "0", 3)));

        calculate(payload)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FITS"))
                .andExpect(jsonPath("$.unallocatedBudget").value(0));
    }

    @Test
    void alreadyFundedGoalsContributeZeroRequirement() throws Exception {
        Map<String, Object> payload = request("PHP", "0", List.of(goal("g1", "100", "150", 12)));

        calculate(payload)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.goalResults[0].status").value("ALREADY_FUNDED"))
                .andExpect(jsonPath("$.totalRequiredMonthlyContribution").value(0))
                .andExpect(jsonPath("$.status").value("FITS"));
    }

    @Test
    void returnsTheCurrentAmountEarmarkingAssumptionAlongsideAShortfallResult() throws Exception {
        Map<String, Object> payload = request("PHP", "120", List.of(
                goal("education", "100", "0", 3), goal("travel", "200", "0", 2)));

        calculate(payload)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentAmountAssumption")
                        .value(MultiGoalFundingCheckCalculator.CURRENT_AMOUNT_ASSUMPTION));
    }

    @Test
    void returnsTheCurrentAmountEarmarkingAssumptionAlongsideAnAlreadyFundedFitsResult() throws Exception {
        Map<String, Object> payload = request("PHP", "0", List.of(goal("g1", "100", "150", 12)));

        calculate(payload)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FITS"))
                .andExpect(jsonPath("$.currentAmountAssumption")
                        .value(MultiGoalFundingCheckCalculator.CURRENT_AMOUNT_ASSUMPTION));
    }

    @Test
    void sumsAggregateContributionsBeyondSeventeenDigitsWithoutTruncation() throws Exception {
        Map<String, Object> payload = request("PHP", "0", List.of(
                goal("g1", "50000000000000000.00", "0", 1),
                goal("g2", "60000000000000000.00", "0", 1)));

        String body = calculate(payload)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Asserted on the raw JSON text (not a jsonPath/double comparison) so this
        // proves the exact serialized decimal, not a double-precision approximation.
        org.assertj.core.api.Assertions.assertThat(body)
                .contains("\"totalRequiredMonthlyContribution\":110000000000000000.00")
                .contains("\"budgetMinusRequired\":-110000000000000000.00")
                .contains("\"shortfall\":110000000000000000.00")
                .contains("\"status\":\"SHORTFALL\"");
    }

    @Test
    void zeroBudgetIsValid() throws Exception {
        Map<String, Object> payload = request("PHP", "0", List.of(goal("g1", "100", "0", 10)));

        calculate(payload)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHORTFALL"));
    }

    @Test
    void normalizesCurrencyToUppercaseInTheResponse() throws Exception {
        Map<String, Object> payload = request("php", "100", List.of(goal("g1", "100", "0", 1)));

        calculate(payload)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("PHP"));
    }

    @Test
    void identicalRequestsProduceIdenticalResponses() throws Exception {
        Map<String, Object> payload = request("PHP", "120", List.of(
                goal("education", "100", "0", 3), goal("travel", "200", "0", 2)));

        String first = calculate(payload).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String second = calculate(payload).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(first).isEqualTo(second);
    }

    @Test
    void rejectsMissingCurrency() throws Exception {
        Map<String, Object> payload = request(null, "100", List.of(goal("g1", "100", "0", 1)));
        payload.remove("currency");

        calculate(payload)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsMalformedCurrency() throws Exception {
        calculate(request("PH", "100", List.of(goal("g1", "100", "0", 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsMissingAvailableMonthlyBudget() throws Exception {
        Map<String, Object> payload = request("PHP", null, List.of(goal("g1", "100", "0", 1)));
        payload.remove("availableMonthlyBudget");

        calculate(payload)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsNegativeAvailableMonthlyBudget() throws Exception {
        calculate(request("PHP", "-1", List.of(goal("g1", "100", "0", 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsEmptyGoalsList() throws Exception {
        calculate(request("PHP", "100", List.of()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsMissingGoalsList() throws Exception {
        Map<String, Object> payload = request("PHP", "100", List.of());
        payload.remove("goals");

        calculate(payload)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsMoreThanFiftyGoals() throws Exception {
        List<Map<String, Object>> goals = new ArrayList<>();
        for (int i = 0; i < 51; i++) {
            goals.add(goal("g" + i, "100", "0", 1));
        }

        calculate(request("PHP", "10000", goals))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void acceptsExactlyFiftyGoals() throws Exception {
        List<Map<String, Object>> goals = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            goals.add(goal("g" + i, "100", "0", 1));
        }

        calculate(request("PHP", "10000", goals))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.goalResults", org.hamcrest.Matchers.hasSize(50)));
    }

    @Test
    void rejectsNullGoalEntry() throws Exception {
        List<Object> goals = new ArrayList<>();
        goals.add(goal("g1", "100", "0", 1));
        goals.add(null);
        Map<String, Object> payload = new HashMap<>();
        payload.put("currency", "PHP");
        payload.put("availableMonthlyBudget", "100");
        payload.put("goals", goals);

        calculate(payload)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsBlankReference() throws Exception {
        calculate(request("PHP", "100", List.of(goal(" ", "100", "0", 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsReferenceLongerThanSixtyFourCharacters() throws Exception {
        calculate(request("PHP", "100", List.of(goal("r".repeat(65), "100", "0", 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsDuplicateReferences() throws Exception {
        calculate(request("PHP", "1000", List.of(
                goal("dup", "100", "0", 1), goal("dup", "50", "0", 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsMissingTargetAmountForAGoal() throws Exception {
        Map<String, Object> badGoal = goal("g1", null, "0", 1);
        badGoal.remove("targetAmount");

        calculate(request("PHP", "100", List.of(badGoal)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsZeroTargetAmountForAGoal() throws Exception {
        calculate(request("PHP", "100", List.of(goal("g1", "0", "0", 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsNegativeCurrentAmountForAGoal() throws Exception {
        calculate(request("PHP", "100", List.of(goal("g1", "100", "-1", 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsExcessiveFractionDigitsOnAGoalAmount() throws Exception {
        calculate(request("PHP", "100", List.of(goal("g1", "100.001", "0", 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsExcessiveIntegerDigitsOnAGoalAmount() throws Exception {
        calculate(request("PHP", "100", List.of(goal("g1", "100000000000000000.00", "0", 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsZeroContributionMonthsForAGoal() throws Exception {
        calculate(request("PHP", "100", List.of(goal("g1", "100", "0", 0))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsContributionMonthsAboveTwelveHundredForAGoal() throws Exception {
        calculate(request("PHP", "100", List.of(goal("g1", "100", "0", 1201))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsFractionalContributionMonthsForAGoal() throws Exception {
        String body = """
                { "currency": "PHP", "availableMonthlyBudget": "100",
                  "goals": [ { "reference": "g1", "targetAmount": "100", "currentAmount": "0", "contributionMonths": 3.5 } ] }
                """;
        mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsContributionMonthsThatOverflowAndNarrowToAValidValue() throws Exception {
        String body = """
                { "currency": "PHP", "availableMonthlyBudget": "100",
                  "goals": [ { "reference": "g1", "targetAmount": "100", "currentAmount": "0", "contributionMonths": 4294967299 } ] }
                """;
        mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsMalformedRequestBody() throws Exception {
        mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content("not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));
    }

    private Map<String, Object> goal(
            String reference, String targetAmount, String currentAmount, Object contributionMonths
    ) {
        Map<String, Object> goal = new HashMap<>();
        goal.put("reference", reference);
        goal.put("targetAmount", targetAmount);
        goal.put("currentAmount", currentAmount);
        goal.put("contributionMonths", contributionMonths);
        return goal;
    }

    private Map<String, Object> request(String currency, String availableMonthlyBudget, List<?> goals) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("currency", currency);
        payload.put("availableMonthlyBudget", availableMonthlyBudget);
        payload.put("goals", goals);
        return payload;
    }

    private ResultActions calculate(Map<String, Object> payload) throws Exception {
        String body = objectMapper.writeValueAsString(payload);
        return mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
