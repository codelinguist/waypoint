package com.waypoint.planning.debtamortization.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Exercises the debt amortization HTTP boundary in isolation: {@link WebMvcTest} loads only the
 * web layer (this controller plus the shared {@code ApiExceptionHandler} advice), so no database
 * or Testcontainers instance is started, matching this feature's no-persistence scope.
 */
@WebMvcTest(controllers = DebtAmortizationController.class)
class DebtAmortizationApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void calculatesPaidOffScheduleForWorkedExample() throws Exception {
        calculate("1000.00", "0", "300.00", "USD")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID_OFF"))
                .andExpect(jsonPath("$.payoffMonths").value(4))
                .andExpect(jsonPath("$.totalPaid").value(1000.00))
                .andExpect(jsonPath("$.totalInterest").value(0.00))
                .andExpect(jsonPath("$.schedule.length()").value(4))
                .andExpect(jsonPath("$.schedule[3].payment").value(100.00))
                .andExpect(jsonPath("$.schedule[3].closingBalance").value(0.00))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    void normalizesLowercaseCurrencyToUppercase() throws Exception {
        calculate("1000.00", "0", "300.00", "usd")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    void returnsNonAmortizingStatusWithoutRowsWhenPaymentDoesNotExceedInterest() throws Exception {
        calculate("1000.00", "0.01", "10.00", "USD")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NON_AMORTIZING"))
                .andExpect(jsonPath("$.payoffMonths").doesNotExist())
                .andExpect(jsonPath("$.schedule.length()").value(0))
                .andExpect(jsonPath("$.totalPaid").value(0.00))
                .andExpect(jsonPath("$.totalInterest").value(0.00));
    }

    @Test
    void returnsPaidOffImmediatelyForZeroPrincipal() throws Exception {
        calculate("0", "0.01", "10.00", "USD")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID_OFF"))
                .andExpect(jsonPath("$.payoffMonths").value(0))
                .andExpect(jsonPath("$.schedule.length()").value(0));
    }

    @Test
    void rejectsMissingPrincipal() throws Exception {
        java.util.Map<String, Object> body = new HashMap<>();
        body.put("monthlyInterestRate", "0.01");
        body.put("monthlyPayment", "10.00");
        body.put("currency", "USD");

        mockMvc.perform(post("/api/planning/debt-amortization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsNegativePrincipal() throws Exception {
        calculate("-1.00", "0.01", "10.00", "USD")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsZeroPayment() throws Exception {
        calculate("1000.00", "0.01", "0.00", "USD")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsNegativePayment() throws Exception {
        calculate("1000.00", "0.01", "-10.00", "USD")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsRateAboveOne() throws Exception {
        calculate("1000.00", "1.01", "10.00", "USD")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsRateWithExcessiveFractionalDigits() throws Exception {
        calculate("1000.00", "0.123456789", "10.00", "USD")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsAmountWithExcessiveFractionalScale() throws Exception {
        calculate("1000.005", "0.01", "10.00", "USD")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsAmountWithPrecisionOverflow() throws Exception {
        calculate("123456789012345678.00", "0.01", "10.00", "USD")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsBlankCurrency() throws Exception {
        calculate("1000.00", "0.01", "10.00", "")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsMalformedCurrency() throws Exception {
        calculate("1000.00", "0.01", "10.00", "US1")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsMalformedRequestBody() throws Exception {
        mockMvc.perform(post("/api/planning/debt-amortization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));
    }

    @Test
    void identicalRequestsReturnIdenticalResults() throws Exception {
        String first = calculate("100.00", "0.01", "60.00", "USD")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String second = calculate("100.00", "0.01", "60.00", "USD")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(first).isEqualTo(second);
    }

    private ResultActions calculate(
            String principal, String monthlyInterestRate, String monthlyPayment, String currency
    ) throws Exception {
        java.util.Map<String, Object> body = new HashMap<>();
        body.put("principal", principal);
        body.put("monthlyInterestRate", monthlyInterestRate);
        body.put("monthlyPayment", monthlyPayment);
        body.put("currency", currency);

        return mockMvc.perform(post("/api/planning/debt-amortization")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }
}
