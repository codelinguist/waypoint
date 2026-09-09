package com.waypoint.scenarios.incomeinterruption.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.waypoint.scenarios.incomeinterruption.IncomeInterruptionScenarioCalculator;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Exercises {@code POST /api/scenarios/income-interruption} through the real MVC stack,
 * including request validation and the feature-local exception handler. This endpoint has no
 * persistence dependency, so the slice is intentionally isolated with {@link WebMvcTest} instead
 * of a full Spring context or a Postgres/Testcontainers boundary.
 */
@WebMvcTest(controllers = IncomeInterruptionScenarioController.class)
@Import({IncomeInterruptionScenarioCalculator.class, IncomeInterruptionScenarioExceptionHandler.class})
class IncomeInterruptionScenarioApiIntegrationTest {

    private static final String ENDPOINT = "/api/scenarios/income-interruption";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void returnsBaselineAndScenarioPathsWithExtraReserveNeeded() throws Exception {
        calculate(request("usd", "100.00", "100.00", "0", "80.00", 3, 2, 2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.baselineRows.length()").value(3))
                .andExpect(jsonPath("$.baselineRows[0].closingCash").value(120.00))
                .andExpect(jsonPath("$.baselineRows[1].closingCash").value(140.00))
                .andExpect(jsonPath("$.baselineRows[2].closingCash").value(160.00))
                .andExpect(jsonPath("$.scenarioRows[0].closingCash").value(120.00))
                .andExpect(jsonPath("$.scenarioRows[1].closingCash").value(40.00))
                .andExpect(jsonPath("$.scenarioRows[2].closingCash").value(-40.00))
                .andExpect(jsonPath("$.additionalOpeningReserveNeeded").value(40.00))
                .andExpect(jsonPath("$.firstNegativeMonth").value(3))
                .andExpect(jsonPath("$.minimumCash").value(-40.00))
                .andExpect(jsonPath("$.endingCash").value(-40.00));
    }

    @Test
    void returnsNullFirstNegativeMonthWhenTheScenarioNeverGoesNegative() throws Exception {
        calculate(request("USD", "100.00", "100.00", "100.00", "80.00", 3, 2, 2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstNegativeMonth").doesNotExist())
                .andExpect(jsonPath("$.additionalOpeningReserveNeeded").value(0.00));
    }

    @Test
    void identicalRequestsProduceIdenticalResponses() throws Exception {
        String first = calculate(request("USD", "100.00", "100.00", "0", "80.00", 3, 2, 2))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String second = calculate(request("USD", "100.00", "100.00", "0", "80.00", 3, 2, 2))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(first).isEqualTo(second);
    }

    @Test
    void rejectsMissingCurrency() throws Exception {
        Map<String, Object> payload = request(null, "100.00", "100.00", "0", "80.00", 3, 2, 2);
        payload.remove("currency");
        calculate(payload)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsMalformedCurrency() throws Exception {
        calculate(request("US", "100.00", "100.00", "0", "80.00", 3, 2, 2))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsMissingOpeningReserve() throws Exception {
        Map<String, Object> payload = request("USD", null, "100.00", "0", "80.00", 3, 2, 2);
        payload.remove("openingReserve");
        calculate(payload)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsNegativeOpeningReserve() throws Exception {
        calculate(request("USD", "-1.00", "100.00", "0", "80.00", 3, 2, 2))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsNegativeNormalMonthlyNetIncome() throws Exception {
        calculate(request("USD", "100.00", "-1.00", "0", "80.00", 3, 2, 2))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsNegativeInterruptedMonthlyNetIncome() throws Exception {
        calculate(request("USD", "100.00", "100.00", "-1.00", "80.00", 3, 2, 2))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsInterruptedIncomeExceedingNormalIncome() throws Exception {
        calculate(request("USD", "100.00", "100.00", "100.01", "80.00", 3, 2, 2))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsNegativeMonthlyExpenses() throws Exception {
        calculate(request("USD", "100.00", "100.00", "0", "-1.00", 3, 2, 2))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsExcessiveFractionDigits() throws Exception {
        calculate(request("USD", "100.001", "100.00", "0", "80.00", 3, 2, 2))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsExcessiveIntegerDigits() throws Exception {
        calculate(request("USD", "100000000000000000.00", "100.00", "0", "80.00", 3, 2, 2))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsZeroHorizonMonths() throws Exception {
        calculate(request("USD", "100.00", "100.00", "0", "80.00", 0, 1, 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsHorizonMonthsAboveTwelveHundred() throws Exception {
        calculate(request("USD", "100.00", "100.00", "0", "80.00", 1201, 1, 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsInterruptionStartMonthAboveHorizon() throws Exception {
        calculate(request("USD", "100.00", "100.00", "0", "80.00", 3, 4, 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsIntervalThatExtendsBeyondTheHorizon() throws Exception {
        calculate(request("USD", "0", "0", "0", "0", 3, 3, 2))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsMissingHorizonMonths() throws Exception {
        Map<String, Object> payload = request("USD", "100.00", "100.00", "0", "80.00", null, 1, 1);
        payload.remove("horizonMonths");
        calculate(payload)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void acceptsHorizonMonthsAtTheLowerAndUpperBounds() throws Exception {
        calculate(request("USD", "0", "0", "0", "0", 1, 1, 1))
                .andExpect(status().isOk());
        calculate(request("USD", "0", "0", "0", "0", 1200, 1, 1200))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsFractionalHorizonMonths() throws Exception {
        String body = """
                { "currency": "USD", "openingReserve": "100.00", "normalMonthlyNetIncome": "100.00",
                  "interruptedMonthlyNetIncome": "0", "monthlyExpenses": "80.00",
                  "horizonMonths": 3.5, "interruptionStartMonth": 1, "interruptionMonths": 1 }
                """;
        mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsMonthsThatOverflowAndNarrowToAValidValue() throws Exception {
        String body = """
                { "currency": "USD", "openingReserve": "100.00", "normalMonthlyNetIncome": "100.00",
                  "interruptedMonthlyNetIncome": "0", "monthlyExpenses": "80.00",
                  "horizonMonths": 4294967299, "interruptionStartMonth": 1, "interruptionMonths": 1 }
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

    private Map<String, Object> request(
            String currency, String openingReserve, String normalMonthlyNetIncome,
            String interruptedMonthlyNetIncome, String monthlyExpenses,
            Object horizonMonths, Object interruptionStartMonth, Object interruptionMonths
    ) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("currency", currency);
        payload.put("openingReserve", openingReserve);
        payload.put("normalMonthlyNetIncome", normalMonthlyNetIncome);
        payload.put("interruptedMonthlyNetIncome", interruptedMonthlyNetIncome);
        payload.put("monthlyExpenses", monthlyExpenses);
        payload.put("horizonMonths", horizonMonths);
        payload.put("interruptionStartMonth", interruptionStartMonth);
        payload.put("interruptionMonths", interruptionMonths);
        return payload;
    }

    private ResultActions calculate(Map<String, Object> payload) throws Exception {
        String body = objectMapper.writeValueAsString(payload);
        return mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
