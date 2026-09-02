package com.waypoint.household;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Exercises the household/person API against a real PostgreSQL instance so the
 * Flyway migration, JPA mapping, and REST boundary are verified together.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class HouseholdApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createsAndRetrievesHousehold() throws Exception {
        String householdId = createHousehold("  Ralph Household  ", "php")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", not(blankOrNullString())))
                .andExpect(jsonPath("$.name").value("Ralph Household"))
                .andExpect(jsonPath("$.baseCurrency").value("PHP"))
                .andExpect(jsonPath("$.createdAt", not(blankOrNullString())))
                .andExpect(jsonPath("$.updatedAt", not(blankOrNullString())))
                .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(householdId).get("id").asText();

        mockMvc.perform(get("/api/households/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Ralph Household"))
                .andExpect(jsonPath("$.baseCurrency").value("PHP"));
    }

    @Test
    void rejectsBlankHouseholdNameWithoutPersisting() throws Exception {
        createHousehold("   ", "PHP")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsMalformedCurrencyCode() throws Exception {
        createHousehold("Ralph Household", "PESO")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void returnsNotFoundForUnknownHousehold() throws Exception {
        mockMvc.perform(get("/api/households/{id}", java.util.UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("HOUSEHOLD_NOT_FOUND"));
    }

    @Test
    void newHouseholdHasEmptyMemberList() throws Exception {
        String id = createHouseholdId("Empty Household", "PHP");

        mockMvc.perform(get("/api/households/{id}/people", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void addsAndListsPeopleInCreationOrder() throws Exception {
        String id = createHouseholdId("Ralph Household", "PHP");

        addPerson(id, "Ralph", "Parent").andExpect(status().isCreated())
                .andExpect(jsonPath("$.householdId").value(id))
                .andExpect(jsonPath("$.name").value("Ralph"))
                .andExpect(jsonPath("$.role").value("Parent"));
        addPerson(id, "Ralph's Wife", "Parent").andExpect(status().isCreated());
        addPerson(id, "Child One", "Child").andExpect(status().isCreated());

        mockMvc.perform(get("/api/households/{id}/people", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].name").value("Ralph"))
                .andExpect(jsonPath("$[1].name").value("Ralph's Wife"))
                .andExpect(jsonPath("$[2].name").value("Child One"));
    }

    @Test
    void rejectsAddingPersonToUnknownHousehold() throws Exception {
        addPerson(java.util.UUID.randomUUID().toString(), "Ralph", "Parent")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("HOUSEHOLD_NOT_FOUND"));
    }

    @Test
    void rejectsBlankPersonFieldsWithoutPersisting() throws Exception {
        String id = createHouseholdId("Ralph Household", "PHP");

        addPerson(id, " ", "Parent")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));

        mockMvc.perform(get("/api/households/{id}/people", id))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void keepsMembersIsolatedBetweenHouseholds() throws Exception {
        String householdOneId = createHouseholdId("Household One", "PHP");
        String householdTwoId = createHouseholdId("Household Two", "PHP");

        addPerson(householdOneId, "Person A", "Parent").andExpect(status().isCreated());
        addPerson(householdTwoId, "Person B", "Parent").andExpect(status().isCreated());

        mockMvc.perform(get("/api/households/{id}/people", householdOneId))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Person A"));

        mockMvc.perform(get("/api/households/{id}/people", householdTwoId))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Person B"));
    }

    @Test
    void allowsDuplicatePersonNamesWithinHousehold() throws Exception {
        String id = createHouseholdId("Ralph Household", "PHP");

        addPerson(id, "Sam", "Parent").andExpect(status().isCreated());
        addPerson(id, "Sam", "Child").andExpect(status().isCreated());

        mockMvc.perform(get("/api/households/{id}/people", id))
                .andExpect(jsonPath("$.length()").value(2));
    }

    private org.springframework.test.web.servlet.ResultActions createHousehold(String name, String baseCurrency)
            throws Exception {
        String body = objectMapper.writeValueAsString(new java.util.HashMap<>() {{
            put("name", name);
            put("baseCurrency", baseCurrency);
        }});
        return mockMvc.perform(post("/api/households")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String createHouseholdId(String name, String baseCurrency) throws Exception {
        String response = createHousehold(name, baseCurrency)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private org.springframework.test.web.servlet.ResultActions addPerson(String householdId, String name, String role)
            throws Exception {
        String body = objectMapper.writeValueAsString(new java.util.HashMap<>() {{
            put("name", name);
            put("role", role);
        }});
        return mockMvc.perform(post("/api/households/{id}/people", householdId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
