package com.waypoint.planning.runway.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
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
 * Exercises the stateless emergency-fund runway endpoint end to end. The
 * Testcontainers PostgreSQL instance below is only present because the
 * Spring context requires a datasource to start at all (Flyway runs on
 * boot); this feature itself performs no database read or write, which
 * {@link #repeatedIdenticalRequestsProduceIdenticalResults()} and the
 * absence of any household/entity identifier on the endpoint both confirm.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class EmergencyFundRunwayApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void returnsFiniteRunwayForAPositiveShortfall() throws Exception {
        calculate("1000.00", "400.00", "100.00", "USD")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.availableReserve").value(1000.00))
                .andExpect(jsonPath("$.monthlyExpenses").value(400.00))
                .andExpect(jsonPath("$.monthlyNetIncome").value(100.00))
                .andExpect(jsonPath("$.monthlyShortfall").value(300.00))
                .andExpect(jsonPath("$.status").value("FINITE"))
                .andExpect(jsonPath("$.runwayMonths").value(3.33))
                .andExpect(jsonPath("$.fullMonthsCovered").value(3))
                .andExpect(jsonPath("$.modelNote").isNotEmpty());
    }

    @Test
    void roundsRunwayMonthsDownRatherThanToTheNearestHundredth() throws Exception {
        calculate("1000.00", "700.00", "100.00", "USD")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyShortfall").value(600.00))
                .andExpect(jsonPath("$.runwayMonths").value(1.66))
                .andExpect(jsonPath("$.fullMonthsCovered").value(1));
    }

    @Test
    void returnsZeroRunwayForAZeroReserveWithAPositiveShortfall() throws Exception {
        calculate("0", "400.00", "100.00", "USD")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINITE"))
                .andExpect(jsonPath("$.runwayMonths").value(0.00))
                .andExpect(jsonPath("$.fullMonthsCovered").value(0));
    }

    @Test
    void returnsNoShortfallWithNullMonthValuesWhenIncomeEqualsExpenses() throws Exception {
        calculate("1000.00", "400.00", "400.00", "USD")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NO_SHORTFALL"))
                .andExpect(jsonPath("$.monthlyShortfall").value(0.00))
                .andExpect(jsonPath("$.runwayMonths").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.fullMonthsCovered").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.modelNote").value(org.hamcrest.Matchers.containsString("NO_SHORTFALL")));
    }

    @Test
    void returnsNoShortfallWithNullMonthValuesWhenIncomeExceedsExpenses() throws Exception {
        calculate("1000.00", "400.00", "500.00", "USD")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NO_SHORTFALL"))
                .andExpect(jsonPath("$.monthlyShortfall").value(0.00))
                .andExpect(jsonPath("$.runwayMonths").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.fullMonthsCovered").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void returnsNoShortfallWhenEveryInputIsZero() throws Exception {
        calculate("0", "0", "0", "USD")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NO_SHORTFALL"))
                .andExpect(jsonPath("$.monthlyShortfall").value(0.00))
                .andExpect(jsonPath("$.runwayMonths").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.fullMonthsCovered").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void normalizesLowercaseCurrencyToUppercase() throws Exception {
        calculate("1000.00", "400.00", "100.00", "usd")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    void identicalRequestsReturnIdenticalResponses() throws Exception {
        String first = calculate("1000.00", "400.00", "100.00", "USD")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String second = calculate("1000.00", "400.00", "100.00", "USD")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(first).isEqualTo(second);
    }

    @Test
    void rejectsMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/api/planning/emergency-fund-runway")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsNegativeAvailableReserve() throws Exception {
        calculate("-1.00", "400.00", "100.00", "USD")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsExcessiveFractionDigits() throws Exception {
        calculate("1000.123", "400.00", "100.00", "USD")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsExcessiveIntegerDigits() throws Exception {
        calculate("100000000000000000.00", "400.00", "100.00", "USD")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsMalformedCurrencyCode() throws Exception {
        calculate("1000.00", "400.00", "100.00", "US1")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsBlankCurrency() throws Exception {
        calculate("1000.00", "400.00", "100.00", "")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsMalformedJsonBody() throws Exception {
        mockMvc.perform(post("/api/planning/emergency-fund-runway")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));
    }

    private ResultActions calculate(
            String availableReserve, String monthlyExpenses, String monthlyNetIncome, String currency
    ) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("availableReserve", new java.math.BigDecimal(availableReserve));
        payload.put("monthlyExpenses", new java.math.BigDecimal(monthlyExpenses));
        payload.put("monthlyNetIncome", new java.math.BigDecimal(monthlyNetIncome));
        payload.put("currency", currency);
        String body = objectMapper.writeValueAsString(payload);
        return mockMvc.perform(post("/api/planning/emergency-fund-runway")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
