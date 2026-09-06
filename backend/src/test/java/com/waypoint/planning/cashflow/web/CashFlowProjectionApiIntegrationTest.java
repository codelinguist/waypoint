package com.waypoint.planning.cashflow.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.waypoint.planning.cashflow.CashFlowProjectionCalculator;
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
 * Exercises {@code POST /api/planning/cash-flow-projection} through the real MVC stack,
 * including request validation and the feature-local exception handler. This endpoint has no
 * persistence dependency, so the slice is intentionally isolated with {@link WebMvcTest} instead
 * of a full Spring context or a Postgres/Testcontainers boundary.
 */
@WebMvcTest(controllers = CashFlowProjectionController.class)
@Import({CashFlowProjectionCalculator.class, CashFlowProjectionExceptionHandler.class})
class CashFlowProjectionApiIntegrationTest {

    private static final String ENDPOINT = "/api/planning/cash-flow-projection";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void returnsADatedProjectionThatBecomesNegative() throws Exception {
        calculate(request("usd", "2027-01", "1000.00", "300.00", "500.00", 6))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.startMonth").value("2027-01"))
                .andExpect(jsonPath("$.rows.length()").value(6))
                .andExpect(jsonPath("$.rows[0].month").value("2027-01"))
                .andExpect(jsonPath("$.rows[0].closingCash").value(800.00))
                .andExpect(jsonPath("$.rows[5].month").value("2027-06"))
                .andExpect(jsonPath("$.rows[5].closingCash").value(-200.00))
                .andExpect(jsonPath("$.endingCash").value(-200.00))
                .andExpect(jsonPath("$.lowestClosingBalance").value(-200.00))
                .andExpect(jsonPath("$.lowestClosingBalanceMonth").value("2027-06"))
                .andExpect(jsonPath("$.firstNegativeMonth").value("2027-06"))
                .andExpect(jsonPath("$.status").value("BECOMES_NEGATIVE"));
    }

    @Test
    void returnsNullFirstNegativeMonthWhenTheBalanceNeverGoesNegative() throws Exception {
        calculate(request("USD", "2027-01", "500.00", "200.00", "100.00", 3))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REMAINS_NONNEGATIVE"))
                .andExpect(jsonPath("$.firstNegativeMonth").doesNotExist());
    }

    @Test
    void identicalRequestsProduceIdenticalResponses() throws Exception {
        String first = calculate(request("USD", "2027-01", "1000.00", "300.00", "500.00", 6))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String second = calculate(request("USD", "2027-01", "1000.00", "300.00", "500.00", 6))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(first).isEqualTo(second);
    }

    @Test
    void rejectsMissingCurrency() throws Exception {
        Map<String, Object> payload = request(null, "2027-01", "1000.00", "0", "0", 1);
        payload.remove("currency");
        calculate(payload)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsMalformedCurrency() throws Exception {
        calculate(request("US", "2027-01", "1000.00", "0", "0", 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsMissingStartMonth() throws Exception {
        Map<String, Object> payload = request("USD", null, "1000.00", "0", "0", 1);
        payload.remove("startMonth");
        calculate(payload)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsMalformedStartMonth() throws Exception {
        calculate(request("USD", "2027-13", "1000.00", "0", "0", 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
        calculate(request("USD", "2027-1", "1000.00", "0", "0", 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
        calculate(request("USD", "27-01", "1000.00", "0", "0", 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsMissingStartingCash() throws Exception {
        Map<String, Object> payload = request("USD", "2027-01", null, "0", "0", 1);
        payload.remove("startingCash");
        calculate(payload)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsNegativeStartingCash() throws Exception {
        calculate(request("USD", "2027-01", "-1.00", "0", "0", 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsNegativeMonthlyInflow() throws Exception {
        calculate(request("USD", "2027-01", "0", "-1.00", "0", 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsNegativeMonthlyOutflow() throws Exception {
        calculate(request("USD", "2027-01", "0", "0", "-1.00", 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsExcessiveFractionDigits() throws Exception {
        calculate(request("USD", "2027-01", "100.001", "0", "0", 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsExcessiveIntegerDigits() throws Exception {
        calculate(request("USD", "2027-01", "100000000000000000.00", "0", "0", 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsZeroMonths() throws Exception {
        calculate(request("USD", "2027-01", "0", "0", "0", 0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsNegativeMonths() throws Exception {
        calculate(request("USD", "2027-01", "0", "0", "0", -1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsMonthsAboveTwelveHundred() throws Exception {
        calculate(request("USD", "2027-01", "0", "0", "0", 1201))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsMissingMonths() throws Exception {
        Map<String, Object> payload = request("USD", "2027-01", "0", "0", "0", 1);
        payload.remove("months");
        calculate(payload)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void acceptsMonthsAtTheLowerAndUpperBounds() throws Exception {
        calculate(request("USD", "2027-01", "0", "0", "0", 1))
                .andExpect(status().isOk());
        calculate(request("USD", "2027-01", "0", "0", "0", 1200))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsFractionalMonths() throws Exception {
        String body = """
                { "currency": "USD", "startMonth": "2027-01", "startingCash": "1000.00",
                  "monthlyInflow": "0", "monthlyOutflow": "0", "months": 3.5 }
                """;
        mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsMonthsThatOverflowAndNarrowToAValidValue() throws Exception {
        String body = """
                { "currency": "USD", "startMonth": "2027-01", "startingCash": "1000.00",
                  "monthlyInflow": "0", "monthlyOutflow": "0", "months": 4294967299 }
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
            String currency, String startMonth, String startingCash,
            String monthlyInflow, String monthlyOutflow, Object months
    ) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("currency", currency);
        payload.put("startMonth", startMonth);
        payload.put("startingCash", startingCash);
        payload.put("monthlyInflow", monthlyInflow);
        payload.put("monthlyOutflow", monthlyOutflow);
        payload.put("months", months);
        return payload;
    }

    private ResultActions calculate(Map<String, Object> payload) throws Exception {
        String body = objectMapper.writeValueAsString(payload);
        return mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
