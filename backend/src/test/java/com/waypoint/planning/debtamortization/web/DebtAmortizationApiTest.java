package com.waypoint.planning.debtamortization.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.waypoint.planning.debtamortization.DebtAmortizationCalculator;
import com.waypoint.planning.debtamortization.DebtAmortizationResult;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

    private static final int HORIZON_MONTHS = 1200;

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
    void returnsNonAmortizingStatusWhenPaymentIsBelowFirstInterest() throws Exception {
        calculate("1000.00", "0.01", "5.00", "USD")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NON_AMORTIZING"))
                .andExpect(jsonPath("$.payoffMonths").doesNotExist())
                .andExpect(jsonPath("$.schedule.length()").value(0));
    }

    @Test
    void returnsPaidOffExactlyAtHorizon() throws Exception {
        calculate("1200.00", "0", "1.00", "USD")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID_OFF"))
                .andExpect(jsonPath("$.payoffMonths").value(HORIZON_MONTHS))
                .andExpect(jsonPath("$.schedule.length()").value(HORIZON_MONTHS))
                .andExpect(jsonPath("$.remainingBalance").value(0.00));
    }

    @Test
    void returnsHorizonLimitWithReconciledPartialTotalsWhenStillPositiveAtHorizon() throws Exception {
        String principal = "1000000.00";
        String monthlyInterestRate = "0.001";
        String monthlyPayment = "1005.00";

        DebtAmortizationResult expected = DebtAmortizationCalculator.calculate(
                new BigDecimal(principal), new BigDecimal(monthlyInterestRate), new BigDecimal(monthlyPayment), "USD");

        calculate(principal, monthlyInterestRate, monthlyPayment, "USD")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("HORIZON_LIMIT"))
                .andExpect(jsonPath("$.payoffMonths").doesNotExist())
                .andExpect(jsonPath("$.schedule.length()").value(HORIZON_MONTHS))
                .andExpect(jsonPath("$.remainingBalance").value(expected.remainingBalance().doubleValue()))
                .andExpect(jsonPath("$.totalPaid").value(expected.totalPaid().doubleValue()))
                .andExpect(jsonPath("$.totalInterest").value(expected.totalInterest().doubleValue()));

        assertThat(expected.remainingBalance()).isGreaterThan(BigDecimal.ZERO);
        assertThat(expected.totalPaid()).isGreaterThan(BigDecimal.ZERO);
        assertThat(expected.totalInterest()).isGreaterThan(BigDecimal.ZERO);
    }

    @ParameterizedTest
    @ValueSource(strings = {"principal", "monthlyInterestRate", "monthlyPayment", "currency"})
    void rejectsMissingRequiredField(String fieldToOmit) throws Exception {
        Map<String, Object> body = validBody();
        body.remove(fieldToOmit);

        mockMvc.perform(post("/api/planning/debt-amortization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"principal", "monthlyInterestRate", "monthlyPayment", "currency"})
    void rejectsExplicitNullRequiredField(String fieldToNull) throws Exception {
        Map<String, Object> body = validBody();
        body.put(fieldToNull, null);

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
    void rejectsNegativeRate() throws Exception {
        calculate("1000.00", "-0.01", "10.00", "USD")
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
    void rejectsPrincipalWithExcessiveFractionalScale() throws Exception {
        calculate("1000.005", "0.01", "10.00", "USD")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsPrincipalWithExcessiveIntegerDigits() throws Exception {
        calculate("123456789012345678.00", "0.01", "10.00", "USD")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsMonthlyPaymentWithExcessiveFractionalScale() throws Exception {
        calculate("1000.00", "0.01", "10.005", "USD")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsMonthlyPaymentWithExcessiveIntegerDigits() throws Exception {
        calculate("1000.00", "0.01", "123456789012345678.00", "USD")
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
        Map<String, Object> body = new HashMap<>();
        body.put("principal", principal);
        body.put("monthlyInterestRate", monthlyInterestRate);
        body.put("monthlyPayment", monthlyPayment);
        body.put("currency", currency);

        return mockMvc.perform(post("/api/planning/debt-amortization")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private static Map<String, Object> validBody() {
        Map<String, Object> body = new HashMap<>();
        body.put("principal", "1000.00");
        body.put("monthlyInterestRate", "0.01");
        body.put("monthlyPayment", "10.00");
        body.put("currency", "USD");
        return body;
    }
}
