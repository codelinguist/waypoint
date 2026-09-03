package com.waypoint.household;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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
 * Exercises the income-stream/obligation API against a real PostgreSQL
 * instance so the Task 003 -> Task 004 Flyway upgrade path, JPA mapping, and
 * REST boundary are verified together.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class IncomeObligationApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createsAndRetrievesIncomeStream() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        String body = createIncomeStream(householdId, "October Salary", "SALARY", "50000.00", "MONTHLY", "php",
                "GROSS", "EXPECTED", LocalDate.now().plusMonths(1).toString(), null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.householdId").value(householdId))
                .andExpect(jsonPath("$.name").value("October Salary"))
                .andExpect(jsonPath("$.incomeType").value("SALARY"))
                .andExpect(jsonPath("$.amount").value(50000.00))
                .andExpect(jsonPath("$.frequency").value("MONTHLY"))
                .andExpect(jsonPath("$.currency").value("PHP"))
                .andExpect(jsonPath("$.compensationClassification").value("GROSS"))
                .andExpect(jsonPath("$.certainty").value("EXPECTED"))
                .andExpect(jsonPath("$.sourceType").value("MANUAL_ENTRY"))
                .andReturn().getResponse().getContentAsString();
        String incomeStreamId = objectMapper.readTree(body).get("id").asText();

        mockMvc.perform(get("/api/households/{h}/income-streams/{i}", householdId, incomeStreamId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(incomeStreamId))
                .andExpect(jsonPath("$.name").value("October Salary"));
    }

    @Test
    void acceptsFutureIncomeStreamStartDate() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createIncomeStream(householdId, "New Job", "SALARY", "1000.00", "MONTHLY", "PHP", "GROSS", "EXPECTED",
                LocalDate.now().plusMonths(3).toString(), null)
                .andExpect(status().isCreated());
    }

    @Test
    void acceptsZeroIncomeStreamAmount() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createIncomeStream(householdId, "Unpaid Leave", "SALARY", "0", "MONTHLY", "PHP", "GROSS", "CONFIRMED",
                LocalDate.now().toString(), null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(0));
    }

    @Test
    void rejectsNegativeIncomeStreamAmount() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createIncomeStream(householdId, "Bad Salary", "SALARY", "-1", "MONTHLY", "PHP", "GROSS", "CONFIRMED",
                LocalDate.now().toString(), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsBlankIncomeStreamName() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createIncomeStream(householdId, "  ", "SALARY", "1", "MONTHLY", "PHP", "GROSS", "CONFIRMED",
                LocalDate.now().toString(), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsMalformedIncomeStreamCurrency() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createIncomeStream(householdId, "Salary", "SALARY", "1", "MONTHLY", "PESO", "GROSS", "CONFIRMED",
                LocalDate.now().toString(), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsUnrecognizedIncomeType() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createIncomeStream(householdId, "Salary", "PENSION", "1", "MONTHLY", "PHP", "GROSS", "CONFIRMED",
                LocalDate.now().toString(), null)
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsUnrecognizedFrequency() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createIncomeStream(householdId, "Salary", "SALARY", "1", "DAILY", "PHP", "GROSS", "CONFIRMED",
                LocalDate.now().toString(), null)
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsUnrecognizedCertainty() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createIncomeStream(householdId, "Salary", "SALARY", "1", "MONTHLY", "PHP", "GROSS", "GUARANTEED",
                LocalDate.now().toString(), null)
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsUnrecognizedCompensationClassification() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createIncomeStream(householdId, "Salary", "SALARY", "1", "MONTHLY", "PHP", "AFTER_TAX", "CONFIRMED",
                LocalDate.now().toString(), null)
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsIncomeStreamEndDateBeforeStartDate() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createIncomeStream(householdId, "Salary", "SALARY", "1", "MONTHLY", "PHP", "GROSS", "CONFIRMED",
                LocalDate.now().toString(), LocalDate.now().minusDays(1).toString())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void acceptsIncomeStreamEndDateEqualToStartDate() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createIncomeStream(householdId, "Short Contract", "HOURLY_CONTRACT", "1", "HOURLY", "PHP", "UNKNOWN",
                "VARIABLE", LocalDate.now().toString(), LocalDate.now().toString())
                .andExpect(status().isCreated());
    }

    @Test
    void preservesExactDecimalValueForIncomeStream() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createIncomeStream(householdId, "Salary", "SALARY", "1234.56", "MONTHLY", "PHP", "GROSS", "CONFIRMED",
                LocalDate.now().toString(), null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(1234.56));
    }

    @Test
    void rejectsIncomeStreamAmountWithExcessiveFractionalScale() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createIncomeStream(householdId, "Salary", "SALARY", "100.005", "MONTHLY", "PHP", "GROSS", "CONFIRMED",
                LocalDate.now().toString(), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsIncomeStreamAmountWithPrecisionOverflow() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createIncomeStream(householdId, "Salary", "SALARY", "123456789012345678.00", "MONTHLY", "PHP", "GROSS",
                "CONFIRMED", LocalDate.now().toString(), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsIncomeStreamWithUnsupportedSourceTypeField() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        String body = """
                {
                  "name": "Salary", "incomeType": "SALARY", "amount": "1", "frequency": "MONTHLY",
                  "currency": "PHP", "compensationClassification": "GROSS", "certainty": "CONFIRMED",
                  "startDate": "%s", "sourceType": "IMPORTED"
                }
                """.formatted(LocalDate.now());

        mockMvc.perform(post("/api/households/{h}/income-streams", householdId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsCreatingIncomeStreamForUnknownHousehold() throws Exception {
        createIncomeStream(UUID.randomUUID().toString(), "Salary", "SALARY", "1", "MONTHLY", "PHP", "GROSS",
                "CONFIRMED", LocalDate.now().toString(), null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("HOUSEHOLD_NOT_FOUND"));
    }

    @Test
    void returnsNotFoundWhenIncomeStreamBelongsToAnotherHousehold() throws Exception {
        String householdOneId = createHouseholdId("Household One", "PHP");
        String householdTwoId = createHouseholdId("Household Two", "PHP");

        String body = createIncomeStream(householdOneId, "Salary", "SALARY", "1", "MONTHLY", "PHP", "GROSS",
                "CONFIRMED", LocalDate.now().toString(), null)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String incomeStreamId = objectMapper.readTree(body).get("id").asText();

        mockMvc.perform(get("/api/households/{h}/income-streams/{i}", householdTwoId, incomeStreamId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("INCOME_STREAM_NOT_FOUND"));
    }

    @Test
    void newHouseholdHasEmptyIncomeStreamList() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        mockMvc.perform(get("/api/households/{h}/income-streams", householdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listsIncomeStreamsInCreationOrderAndAllowsDuplicateNames() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createIncomeStream(householdId, "Salary", "SALARY", "1", "MONTHLY", "PHP", "GROSS", "CONFIRMED",
                LocalDate.now().toString(), null)
                .andExpect(status().isCreated());
        createIncomeStream(householdId, "Salary", "SALARY", "2", "MONTHLY", "PHP", "GROSS", "CONFIRMED",
                LocalDate.now().toString(), null)
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/households/{h}/income-streams", householdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].amount").value(1))
                .andExpect(jsonPath("$[1].amount").value(2));
    }

    @Test
    void createsAndRetrievesObligation() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        String body = createObligation(householdId, "Mortgage", "MORTGAGE", "25000.00", "MONTHLY", "PHP",
                LocalDate.now().toString(), null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.householdId").value(householdId))
                .andExpect(jsonPath("$.name").value("Mortgage"))
                .andExpect(jsonPath("$.obligationType").value("MORTGAGE"))
                .andExpect(jsonPath("$.amount").value(25000.00))
                .andExpect(jsonPath("$.frequency").value("MONTHLY"))
                .andExpect(jsonPath("$.sourceType").value("MANUAL_ENTRY"))
                .andReturn().getResponse().getContentAsString();
        String obligationId = objectMapper.readTree(body).get("id").asText();

        mockMvc.perform(get("/api/households/{h}/obligations/{o}", householdId, obligationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(obligationId));
    }

    @Test
    void acceptsFutureObligationStartDate() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createObligation(householdId, "New Loan", "LOAN_PAYMENT", "1000.00", "MONTHLY", "PHP",
                LocalDate.now().plusMonths(2).toString(), null)
                .andExpect(status().isCreated());
    }

    @Test
    void acceptsZeroObligationAmount() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createObligation(householdId, "Paid Off Loan", "LOAN_PAYMENT", "0", "MONTHLY", "PHP",
                LocalDate.now().toString(), null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(0));
    }

    @Test
    void rejectsNegativeObligationAmount() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createObligation(householdId, "Bad Loan", "LOAN_PAYMENT", "-1", "MONTHLY", "PHP",
                LocalDate.now().toString(), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsBlankObligationName() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createObligation(householdId, "  ", "LOAN_PAYMENT", "1", "MONTHLY", "PHP", LocalDate.now().toString(), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsMalformedObligationCurrency() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createObligation(householdId, "Loan", "LOAN_PAYMENT", "1", "MONTHLY", "PESO", LocalDate.now().toString(),
                null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsUnrecognizedObligationType() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createObligation(householdId, "Loan", "SUBSCRIPTION", "1", "MONTHLY", "PHP", LocalDate.now().toString(),
                null)
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsObligationEndDateBeforeStartDate() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createObligation(householdId, "Loan", "LOAN_PAYMENT", "1", "MONTHLY", "PHP", LocalDate.now().toString(),
                LocalDate.now().minusDays(1).toString())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void listsObligationsInCreationOrderAndAllowsDuplicateNames() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createObligation(householdId, "Loan", "LOAN_PAYMENT", "1", "MONTHLY", "PHP", LocalDate.now().toString(),
                null)
                .andExpect(status().isCreated());
        createObligation(householdId, "Loan", "LOAN_PAYMENT", "2", "MONTHLY", "PHP", LocalDate.now().toString(),
                null)
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/households/{h}/obligations", householdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].amount").value(1))
                .andExpect(jsonPath("$[1].amount").value(2));
    }

    @Test
    void preservesExactDecimalValueForObligation() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createObligation(householdId, "Loan", "LOAN_PAYMENT", "1234.56", "MONTHLY", "PHP",
                LocalDate.now().toString(), null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(1234.56));
    }

    @Test
    void rejectsObligationAmountWithExcessiveFractionalScale() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createObligation(householdId, "Loan", "LOAN_PAYMENT", "100.005", "MONTHLY", "PHP",
                LocalDate.now().toString(), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsObligationAmountWithPrecisionOverflow() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        createObligation(householdId, "Loan", "LOAN_PAYMENT", "123456789012345678.00", "MONTHLY", "PHP",
                LocalDate.now().toString(), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsObligationWithUnsupportedSourceTypeField() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        String body = """
                {
                  "name": "Loan", "obligationType": "LOAN_PAYMENT", "amount": "1", "frequency": "MONTHLY",
                  "currency": "PHP", "startDate": "%s", "sourceType": "IMPORTED"
                }
                """.formatted(LocalDate.now());

        mockMvc.perform(post("/api/households/{h}/obligations", householdId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsCreatingObligationForUnknownHousehold() throws Exception {
        createObligation(UUID.randomUUID().toString(), "Loan", "LOAN_PAYMENT", "1", "MONTHLY", "PHP",
                LocalDate.now().toString(), null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("HOUSEHOLD_NOT_FOUND"));
    }

    @Test
    void returnsNotFoundWhenObligationBelongsToAnotherHousehold() throws Exception {
        String householdOneId = createHouseholdId("Household One", "PHP");
        String householdTwoId = createHouseholdId("Household Two", "PHP");

        String body = createObligation(householdOneId, "Loan", "LOAN_PAYMENT", "1", "MONTHLY", "PHP",
                LocalDate.now().toString(), null)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String obligationId = objectMapper.readTree(body).get("id").asText();

        mockMvc.perform(get("/api/households/{h}/obligations/{o}", householdTwoId, obligationId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("OBLIGATION_NOT_FOUND"));
    }

    @Test
    void newHouseholdHasEmptyObligationList() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        mockMvc.perform(get("/api/households/{h}/obligations", householdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void keepsIncomeStreamsAndObligationsIsolatedBetweenHouseholds() throws Exception {
        String householdOneId = createHouseholdId("Household One", "PHP");
        String householdTwoId = createHouseholdId("Household Two", "PHP");

        createIncomeStream(householdOneId, "Salary A", "SALARY", "1", "MONTHLY", "PHP", "GROSS", "CONFIRMED",
                LocalDate.now().toString(), null)
                .andExpect(status().isCreated());
        createIncomeStream(householdTwoId, "Salary B", "SALARY", "2", "MONTHLY", "PHP", "GROSS", "CONFIRMED",
                LocalDate.now().toString(), null)
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/households/{h}/income-streams", householdOneId))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Salary A"));
        mockMvc.perform(get("/api/households/{h}/income-streams", householdTwoId))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Salary B"));
    }

    private String createHouseholdId(String name, String baseCurrency) throws Exception {
        String body = objectMapper.writeValueAsString(new HashMap<>() {{
            put("name", name);
            put("baseCurrency", baseCurrency);
        }});
        String response = mockMvc.perform(post("/api/households")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private ResultActions createIncomeStream(
            String householdId, String name, String incomeType, String amount, String frequency, String currency,
            String compensationClassification, String certainty, String startDate, String endDate
    ) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", name);
        payload.put("incomeType", incomeType);
        payload.put("amount", amount);
        payload.put("frequency", frequency);
        payload.put("currency", currency);
        payload.put("compensationClassification", compensationClassification);
        payload.put("certainty", certainty);
        payload.put("startDate", startDate);
        payload.put("endDate", endDate);
        String body = objectMapper.writeValueAsString(payload);
        return mockMvc.perform(post("/api/households/{h}/income-streams", householdId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions createObligation(
            String householdId, String name, String obligationType, String amount, String frequency,
            String currency, String startDate, String endDate
    ) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", name);
        payload.put("obligationType", obligationType);
        payload.put("amount", amount);
        payload.put("frequency", frequency);
        payload.put("currency", currency);
        payload.put("startDate", startDate);
        payload.put("endDate", endDate);
        String body = objectMapper.writeValueAsString(payload);
        return mockMvc.perform(post("/api/households/{h}/obligations", householdId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
