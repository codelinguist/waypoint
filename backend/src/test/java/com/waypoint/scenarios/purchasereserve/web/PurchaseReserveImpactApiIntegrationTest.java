package com.waypoint.scenarios.purchasereserve.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.waypoint.planning.runway.EmergencyFundRunwayCalculator;
import com.waypoint.scenarios.purchasereserve.PurchaseReserveImpactCalculator;
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
 * Exercises {@code POST /api/scenarios/purchase-reserve-impact} through the
 * real MVC stack, including request validation and the controller-local
 * exception handler. This endpoint has no persistence dependency, so the
 * slice is intentionally isolated with {@link WebMvcTest} instead of a full
 * Spring context or a Postgres/Testcontainers boundary, matching the
 * emergency-fund-runway and goal-contribution-calculator precedent.
 */
@WebMvcTest(controllers = PurchaseReserveImpactController.class)
@Import({PurchaseReserveImpactCalculator.class, EmergencyFundRunwayCalculator.class})
class PurchaseReserveImpactApiIntegrationTest {

    private static final String ENDPOINT = "/api/scenarios/purchase-reserve-impact";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void returnsTheDocumentedPrimaryScenario() throws Exception {
        calculate(request("PHP", "1000.00", "400.00", "300.00", "100.00", "800.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("PHP"))
                .andExpect(jsonPath("$.reserveAfterPurchase").value(600.00))
                .andExpect(jsonPath("$.purchaseFundingGap").value(0.00))
                .andExpect(jsonPath("$.purchaseFitsAvailableCash").value(true))
                .andExpect(jsonPath("$.baselineReserveFloorGap").value(0.00))
                .andExpect(jsonPath("$.reserveFloorGapAfterPurchase").value(200.00))
                .andExpect(jsonPath("$.reserveMeetsFloorAfterPurchase").value(false))
                .andExpect(jsonPath("$.beforePurchaseRunway.status").value("FINITE"))
                .andExpect(jsonPath("$.beforePurchaseRunway.runwayMonths").value(5.00))
                .andExpect(jsonPath("$.afterPurchaseRunwayAvailability").value("AVAILABLE"))
                .andExpect(jsonPath("$.afterPurchaseRunway.status").value("FINITE"))
                .andExpect(jsonPath("$.afterPurchaseRunway.runwayMonths").value(3.00));
    }

    @Test
    void reportsInsufficientCashAndOmitsAfterRunwayWhenPurchaseExceedsReserve() throws Exception {
        calculate(request("USD", "1000.00", "1200.00", "300.00", "100.00", "800.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reserveAfterPurchase").value(-200.00))
                .andExpect(jsonPath("$.purchaseFundingGap").value(200.00))
                .andExpect(jsonPath("$.purchaseFitsAvailableCash").value(false))
                .andExpect(jsonPath("$.afterPurchaseRunwayAvailability").value("INSUFFICIENT_CASH"))
                .andExpect(jsonPath("$.afterPurchaseRunway").doesNotExist());
    }

    @Test
    void preservesNoShortfallNullSemanticsWhenIncomeCoversExpenses() throws Exception {
        calculate(request("USD", "1000.00", "200.00", "300.00", "400.00", "100.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.beforePurchaseRunway.status").value("NO_SHORTFALL"))
                .andExpect(jsonPath("$.beforePurchaseRunway.runwayMonths").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.afterPurchaseRunway.status").value("NO_SHORTFALL"))
                .andExpect(jsonPath("$.afterPurchaseRunway.runwayMonths").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void identicalRequestsProduceIdenticalResponses() throws Exception {
        Map<String, Object> payload = request("PHP", "1000.00", "400.00", "300.00", "100.00", "800.00");
        String first = calculate(payload).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String second = calculate(payload).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(first).isEqualTo(second);
    }

    @Test
    void rejectsMissingCurrency() throws Exception {
        Map<String, Object> payload = request(null, "1000.00", "400.00", "300.00", "100.00", "800.00");
        payload.remove("currency");
        calculate(payload)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsMalformedCurrency() throws Exception {
        calculate(request("PH", "1000.00", "400.00", "300.00", "100.00", "800.00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsNegativePurchaseAmount() throws Exception {
        calculate(request("USD", "1000.00", "-1.00", "300.00", "100.00", "800.00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsNegativeMinimumReserve() throws Exception {
        calculate(request("USD", "1000.00", "400.00", "300.00", "100.00", "-1.00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsMissingMinimumReserve() throws Exception {
        Map<String, Object> payload = request("USD", "1000.00", "400.00", "300.00", "100.00", null);
        payload.remove("minimumReserve");
        calculate(payload)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsExcessiveFractionDigits() throws Exception {
        calculate(request("USD", "1000.001", "400.00", "300.00", "100.00", "800.00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsExcessiveIntegerDigits() throws Exception {
        calculate(request("USD", "100000000000000000.00", "400.00", "300.00", "100.00", "800.00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsMalformedRequestBody() throws Exception {
        mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content("not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));
    }

    @Test
    void acceptsAZeroPurchaseAndAZeroFloor() throws Exception {
        calculate(request("USD", "500.00", "0", "200.00", "50.00", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reserveAfterPurchase").value(500.00))
                .andExpect(jsonPath("$.reserveFloorGapAfterPurchase").value(0.00));
    }

    private Map<String, Object> request(
            String currency, String availableReserve, String purchaseAmount,
            String monthlyExpenses, String monthlyNetIncome, String minimumReserve
    ) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("currency", currency);
        payload.put("availableReserve", availableReserve);
        payload.put("purchaseAmount", purchaseAmount);
        payload.put("monthlyExpenses", monthlyExpenses);
        payload.put("monthlyNetIncome", monthlyNetIncome);
        payload.put("minimumReserve", minimumReserve);
        return payload;
    }

    private ResultActions calculate(Map<String, Object> payload) throws Exception {
        String body = objectMapper.writeValueAsString(payload);
        return mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
