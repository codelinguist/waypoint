package com.waypoint.planning.futurevalue.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.waypoint.planning.futurevalue.FutureValueCalculator;
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
 * Exercises {@code POST /api/planning/future-value} through the real MVC stack, including
 * request validation and the feature-local exception handler. This endpoint has no persistence
 * dependency, so the slice is intentionally isolated with {@link WebMvcTest} instead of a full
 * Spring context or a Postgres/Testcontainers boundary.
 */
@WebMvcTest(controllers = FutureValueController.class)
@Import({FutureValueCalculator.class, FutureValueExceptionHandler.class})
class FutureValueApiIntegrationTest {

    private static final String ENDPOINT = "/api/planning/future-value";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void followsMonthlySequencingExactlyForTheWorkedExample() throws Exception {
        calculate(request("usd", "1000.00", "100.00", "12.00", 2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.schedule[0].openingBalance").value(1000.00))
                .andExpect(jsonPath("$.schedule[0].growth").value(10.00))
                .andExpect(jsonPath("$.schedule[0].closingBalance").value(1110.00))
                .andExpect(jsonPath("$.schedule[1].growth").value(11.10))
                .andExpect(jsonPath("$.schedule[1].closingBalance").value(1221.10))
                .andExpect(jsonPath("$.endingValue").value(1221.10))
                .andExpect(jsonPath("$.totalContributed").value(1200.00))
                .andExpect(jsonPath("$.totalGrowth").value(21.10))
                .andExpect(jsonPath("$.conventions").isNotEmpty());
    }

    @Test
    void zeroRateEndingValueEqualsPrincipalPlusAllContributions() throws Exception {
        calculate(request("USD", "500.00", "50.00", "0", 6))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endingValue").value(800.00))
                .andExpect(jsonPath("$.totalGrowth").value(0));
    }

    @Test
    void allZeroMoneyInputsProduceAValidZeroSchedule() throws Exception {
        calculate(request("USD", "0", "0", "5.00", 3))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endingValue").value(0))
                .andExpect(jsonPath("$.schedule[2].closingBalance").value(0));
    }

    @Test
    void normalizesCurrencyToUppercaseInTheResponse() throws Exception {
        calculate(request("usd", "0", "0", "0", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    void identicalRequestsProduceIdenticalResponses() throws Exception {
        String first = calculate(request("USD", "1000.00", "100.00", "12.00", 6))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String second = calculate(request("USD", "1000.00", "100.00", "12.00", 6))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(first).isEqualTo(second);
    }

    @Test
    void rejectsMissingCurrency() throws Exception {
        Map<String, Object> payload = request(null, "0", "0", "0", 1);
        payload.remove("currency");
        calculate(payload)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsMalformedCurrency() throws Exception {
        calculate(request("US", "0", "0", "0", 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsNegativeStartingPrincipal() throws Exception {
        calculate(request("USD", "-0.01", "0", "0", 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsNegativeMonthlyContribution() throws Exception {
        calculate(request("USD", "0", "-0.01", "0", 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsNegativeAnnualRate() throws Exception {
        calculate(request("USD", "0", "0", "-0.01", 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsExcessiveFractionDigitsOnMoneyFields() throws Exception {
        calculate(request("USD", "100.001", "0", "0", 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsExcessiveIntegerDigitsOnMoneyFields() throws Exception {
        calculate(request("USD", "100000000000000000.00", "0", "0", 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsExcessiveFractionDigitsOnAnnualRate() throws Exception {
        calculate(request("USD", "0", "0", "1.00001", 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsExcessiveIntegerDigitsOnAnnualRate() throws Exception {
        calculate(request("USD", "0", "0", "1000.00", 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsMissingProjectionMonths() throws Exception {
        Map<String, Object> payload = request("USD", "0", "0", "0", 1);
        payload.remove("projectionMonths");
        calculate(payload)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsZeroProjectionMonths() throws Exception {
        calculate(request("USD", "0", "0", "0", 0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsNegativeProjectionMonths() throws Exception {
        calculate(request("USD", "0", "0", "0", -1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsProjectionMonthsAboveTwelveHundred() throws Exception {
        calculate(request("USD", "0", "0", "0", 1201))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void acceptsProjectionMonthsAtTheLowerAndUpperBounds() throws Exception {
        calculate(request("USD", "0", "0", "0", 1))
                .andExpect(status().isOk());
        calculate(request("USD", "0", "0", "0", 1200))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsFractionalProjectionMonths() throws Exception {
        String body = """
                { "currency": "USD", "startingPrincipal": "0", "monthlyContribution": "0",
                  "annualRatePercentage": "0", "projectionMonths": 3.5 }
                """;
        mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));
    }

    @Test
    void rejectsProjectionMonthsThatOverflowAndNarrowToAValidValue() throws Exception {
        String body = """
                { "currency": "USD", "startingPrincipal": "0", "monthlyContribution": "0",
                  "annualRatePercentage": "0", "projectionMonths": 4294967299 }
                """;
        mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));
    }

    @Test
    void rejectsMalformedRequestBody() throws Exception {
        mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content("not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));
    }

    private Map<String, Object> request(
            String currency, String startingPrincipal, String monthlyContribution,
            String annualRatePercentage, Object projectionMonths
    ) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("currency", currency);
        payload.put("startingPrincipal", startingPrincipal);
        payload.put("monthlyContribution", monthlyContribution);
        payload.put("annualRatePercentage", annualRatePercentage);
        payload.put("projectionMonths", projectionMonths);
        return payload;
    }

    private ResultActions calculate(Map<String, Object> payload) throws Exception {
        String body = objectMapper.writeValueAsString(payload);
        return mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
