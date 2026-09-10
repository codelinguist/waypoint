package com.waypoint.scenarios.debtprepayment.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Exercises the debt prepayment comparison HTTP boundary in isolation: {@link WebMvcTest} loads
 * only the web layer (this controller plus its feature-scoped exception advice), so no database or
 * Testcontainers instance is started, matching this feature's no-persistence scope.
 */
@WebMvcTest(controllers = DebtPrepaymentComparisonController.class)
class DebtPrepaymentComparisonApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void comparesWorkedExampleWithZeroInterest() throws Exception {
        compare("1000.00", "0", "300.00", "USD", "400.00")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseline.status").value("PAID_OFF"))
                .andExpect(jsonPath("$.baseline.payoffMonths").value(4))
                .andExpect(jsonPath("$.scenario.status").value("PAID_OFF"))
                .andExpect(jsonPath("$.scenario.payoffMonths").value(2))
                .andExpect(jsonPath("$.scenarioTotalCashPaid").value(1000.00))
                .andExpect(jsonPath("$.lifetimeInterestSaved").value(0.00))
                .andExpect(jsonPath("$.payoffMonthsSaved").value(2))
                .andExpect(jsonPath("$.lifetimeCashSaved").value(0.00))
                .andExpect(jsonPath("$.comparisonUnavailableReason").doesNotExist());
    }

    @Test
    void prepaymentEqualToPrincipalRetainsUpfrontCash() throws Exception {
        compare("100.00", "0.01", "60.00", "USD", "100.00")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scenario.status").value("PAID_OFF"))
                .andExpect(jsonPath("$.scenario.payoffMonths").value(0))
                .andExpect(jsonPath("$.scenario.schedule.length()").value(0))
                .andExpect(jsonPath("$.scenarioTotalCashPaid").value(100.00))
                .andExpect(jsonPath("$.lifetimeInterestSaved").value(1.41))
                .andExpect(jsonPath("$.payoffMonthsSaved").value(2))
                .andExpect(jsonPath("$.lifetimeCashSaved").value(1.41));
    }

    @Test
    void nonAmortizingBaselineSuppressesLifetimeComparison() throws Exception {
        compare("1000.00", "0.01", "10.00", "USD", "0.00")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseline.status").value("NON_AMORTIZING"))
                .andExpect(jsonPath("$.scenario.status").value("NON_AMORTIZING"))
                .andExpect(jsonPath("$.lifetimeInterestSaved").doesNotExist())
                .andExpect(jsonPath("$.payoffMonthsSaved").doesNotExist())
                .andExpect(jsonPath("$.lifetimeCashSaved").doesNotExist())
                .andExpect(jsonPath("$.comparisonUnavailableReason").exists());
    }

    @Test
    void prepaymentThatMakesOnlyScenarioRepayableSuppressesLifetimeComparison() throws Exception {
        compare("1000000.00", "0.001", "1005.00", "USD", "999000.00")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseline.status").value("HORIZON_LIMIT"))
                .andExpect(jsonPath("$.scenario.status").value("PAID_OFF"))
                .andExpect(jsonPath("$.lifetimeInterestSaved").doesNotExist())
                .andExpect(jsonPath("$.comparisonUnavailableReason").exists());
    }

    @Test
    void normalizesLowercaseCurrencyToUppercase() throws Exception {
        compare("1000.00", "0", "300.00", "usd", "400.00")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.baseline.status").value("PAID_OFF"));
    }

    @Test
    void rejectsNegativePrepayment() throws Exception {
        compare("1000.00", "0.01", "10.00", "USD", "-1.00")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsPrepaymentAbovePrincipal() throws Exception {
        compare("1000.00", "0.01", "10.00", "USD", "1000.01")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsPrepaymentWithExcessiveFractionalScale() throws Exception {
        compare("1000.00", "0.01", "10.00", "USD", "100.005")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsPrepaymentWithExcessiveIntegerDigits() throws Exception {
        compare("123456789012345678.00", "0.01", "10.00", "USD", "123456789012345678.00")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsNegativePrincipal() throws Exception {
        compare("-1.00", "0.01", "10.00", "USD", "0.00")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsNegativeRate() throws Exception {
        compare("1000.00", "-0.01", "10.00", "USD", "0.00")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsZeroPayment() throws Exception {
        compare("1000.00", "0.01", "0.00", "USD", "0.00")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsMalformedCurrency() throws Exception {
        compare("1000.00", "0.01", "10.00", "US1", "0.00")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"principal", "monthlyInterestRate", "monthlyPayment", "currency", "immediatePrepayment"})
    void rejectsMissingRequiredField(String fieldToOmit) throws Exception {
        Map<String, Object> body = validBody();
        body.remove(fieldToOmit);

        mockMvc.perform(post("/api/scenarios/debt-prepayment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"principal", "monthlyInterestRate", "monthlyPayment", "currency", "immediatePrepayment"})
    void rejectsExplicitNullRequiredField(String fieldToNull) throws Exception {
        Map<String, Object> body = validBody();
        body.put(fieldToNull, null);

        mockMvc.perform(post("/api/scenarios/debt-prepayment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsMalformedRequestBody() throws Exception {
        mockMvc.perform(post("/api/scenarios/debt-prepayment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));
    }

    @Test
    void identicalRequestsReturnIdenticalResults() throws Exception {
        String first = compare("1000.00", "0", "300.00", "USD", "400.00")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String second = compare("1000.00", "0", "300.00", "USD", "400.00")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Assertions.assertThat(first).isEqualTo(second);
    }

    private ResultActions compare(
            String principal, String monthlyInterestRate, String monthlyPayment, String currency,
            String immediatePrepayment
    ) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("principal", principal);
        body.put("monthlyInterestRate", monthlyInterestRate);
        body.put("monthlyPayment", monthlyPayment);
        body.put("currency", currency);
        body.put("immediatePrepayment", immediatePrepayment);

        return mockMvc.perform(post("/api/scenarios/debt-prepayment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private static Map<String, Object> validBody() {
        Map<String, Object> body = new HashMap<>();
        body.put("principal", "1000.00");
        body.put("monthlyInterestRate", "0.01");
        body.put("monthlyPayment", "10.00");
        body.put("currency", "USD");
        body.put("immediatePrepayment", "100.00");
        return body;
    }
}
