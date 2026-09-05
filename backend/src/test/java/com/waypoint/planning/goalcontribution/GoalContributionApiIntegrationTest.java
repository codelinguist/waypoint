package com.waypoint.planning.goalcontribution;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.waypoint.planning.goalcontribution.web.GoalContributionController;
import com.waypoint.planning.goalcontribution.web.GoalContributionExceptionHandler;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Exercises {@code POST /api/planning/goal-contribution-calculator} through
 * the real MVC stack, including request validation and the feature-local
 * exception handler. This endpoint has no persistence dependency, so the
 * slice is intentionally isolated with {@link WebMvcTest} instead of a full
 * Spring context or a Postgres/Testcontainers boundary.
 */
@WebMvcTest(controllers = GoalContributionController.class)
@Import({GoalContributionCalculator.class, GoalContributionExceptionHandler.class})
class GoalContributionApiIntegrationTest {

    private static final String ENDPOINT = "/api/planning/goal-contribution-calculator";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void returnsEqualMonthlyContributionsForAnUnfundedGoal() throws Exception {
        calculate(request("PHP", "1000.00", "100.00", 3))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("PHP"))
                .andExpect(jsonPath("$.remainingAmount").value(900.00))
                .andExpect(jsonPath("$.monthlyContribution").value(300.00))
                .andExpect(jsonPath("$.totalContributions").value(900.00))
                .andExpect(jsonPath("$.projectedAmount").value(1000.00))
                .andExpect(jsonPath("$.amountAboveTarget").value(0))
                .andExpect(jsonPath("$.status").value("CONTRIBUTIONS_REQUIRED"));
    }

    @Test
    void roundsMonthlyContributionUpAndReportsTheResultingExcess() throws Exception {
        calculate(request("PHP", "100.00", "0", 3))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyContribution").value(33.34))
                .andExpect(jsonPath("$.totalContributions").value(100.02))
                .andExpect(jsonPath("$.amountAboveTarget").value(0.02));
    }

    @Test
    void returnsAlreadyFundedAndPreservesSurplusWhenAtOrAboveTarget() throws Exception {
        calculate(request("PHP", "500.00", "650.00", 12))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ALREADY_FUNDED"))
                .andExpect(jsonPath("$.remainingAmount").value(0))
                .andExpect(jsonPath("$.monthlyContribution").value(0))
                .andExpect(jsonPath("$.totalContributions").value(0))
                .andExpect(jsonPath("$.amountAboveTarget").value(150.00));
    }

    @Test
    void normalizesCurrencyToUppercaseInTheResponse() throws Exception {
        calculate(request("php", "100.00", "0", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("PHP"));
    }

    @Test
    void identicalRequestsProduceIdenticalResponses() throws Exception {
        String first = calculate(request("PHP", "1000.00", "100.00", 3))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String second = calculate(request("PHP", "1000.00", "100.00", 3))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(first).isEqualTo(second);
    }

    @Test
    void rejectsMissingCurrency() throws Exception {
        Map<String, Object> payload = request(null, "100.00", "0", 3);
        payload.remove("currency");
        calculate(payload)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsMalformedCurrency() throws Exception {
        calculate(request("PH", "100.00", "0", 3))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsMissingTargetAmount() throws Exception {
        Map<String, Object> payload = request("PHP", null, "0", 3);
        payload.remove("targetAmount");
        calculate(payload)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsZeroTargetAmount() throws Exception {
        calculate(request("PHP", "0", "0", 3))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsNegativeCurrentAmount() throws Exception {
        calculate(request("PHP", "100.00", "-1.00", 3))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsExcessiveFractionDigits() throws Exception {
        calculate(request("PHP", "100.001", "0", 3))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsExcessiveIntegerDigits() throws Exception {
        calculate(request("PHP", "100000000000000000.00", "0", 3))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsZeroContributionMonths() throws Exception {
        calculate(request("PHP", "100.00", "0", 0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsNegativeContributionMonths() throws Exception {
        calculate(request("PHP", "100.00", "0", -1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsContributionMonthsAboveTwelveHundred() throws Exception {
        calculate(request("PHP", "100.00", "0", 1201))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsFractionalContributionMonths() throws Exception {
        String body = """
                { "currency": "PHP", "targetAmount": "100.00", "currentAmount": "0", "contributionMonths": 3.5 }
                """;
        mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsContributionMonthsThatOverflowAndNarrowToAValidValue() throws Exception {
        String body = """
                { "currency": "PHP", "targetAmount": "100.00", "currentAmount": "0", "contributionMonths": 4294967299 }
                """;
        mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsNegativeContributionMonthsThatUnderflowAndNarrowToAValidValue() throws Exception {
        String body = """
                { "currency": "PHP", "targetAmount": "100.00", "currentAmount": "0", "contributionMonths": -4294967293 }
                """;
        mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsContributionMonthsBeyondTheLongRange() throws Exception {
        String body = """
                { "currency": "PHP", "targetAmount": "100.00", "currentAmount": "0",
                  "contributionMonths": 99999999999999999999999999999999 }
                """;
        mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void acceptsContributionMonthsAtTheLowerAndUpperBounds() throws Exception {
        calculate(request("PHP", "100.00", "0", 1))
                .andExpect(status().isOk());
        calculate(request("PHP", "1200.00", "0", 1200))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsMissingContributionMonths() throws Exception {
        Map<String, Object> payload = request("PHP", "100.00", "0", 3);
        payload.remove("contributionMonths");
        calculate(payload)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsMalformedRequestBody() throws Exception {
        mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content("not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));
    }

    private Map<String, Object> request(
            String currency, String targetAmount, String currentAmount, Object contributionMonths
    ) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("currency", currency);
        payload.put("targetAmount", targetAmount);
        payload.put("currentAmount", currentAmount);
        payload.put("contributionMonths", contributionMonths);
        return payload;
    }

    private org.springframework.test.web.servlet.ResultActions calculate(Map<String, Object> payload) throws Exception {
        String body = objectMapper.writeValueAsString(payload);
        return mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
