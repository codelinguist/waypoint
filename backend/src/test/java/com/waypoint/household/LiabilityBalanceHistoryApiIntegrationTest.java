package com.waypoint.household;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
 * Exercises the liability balance-replacement/history API (WAP-17) against a
 * real PostgreSQL instance: atomic current-row replacement plus immutable
 * audit append, revision-based optimistic concurrency, and deterministic
 * history ordering.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class LiabilityBalanceHistoryApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LiabilityService liabilityService;

    @Autowired
    private LiabilityRepository liabilityRepository;

    @Autowired
    private LiabilityBalanceHistoryRepository liabilityBalanceHistoryRepository;

    @Test
    void recordsBalanceReplacementAndAppendsHistory() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        String liabilityId = createLiabilityId(householdId, "Credit Card", "CREDIT_CARD", "500.00", "PHP",
                "2026-01-01");

        recordBalance(householdId, liabilityId, "300.00", "2026-02-01", "Paid down", 0)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.liabilityId").value(liabilityId))
                .andExpect(jsonPath("$.householdId").value(householdId))
                .andExpect(jsonPath("$.currency").value("PHP"))
                .andExpect(jsonPath("$.previousBalance").value("500.00"))
                .andExpect(jsonPath("$.previousBalanceAsOf").value("2026-01-01"))
                .andExpect(jsonPath("$.newBalance").value("300.00"))
                .andExpect(jsonPath("$.newBalanceAsOf").value("2026-02-01"))
                .andExpect(jsonPath("$.newSourceType").value("MANUAL_ENTRY"))
                .andExpect(jsonPath("$.reason").value("Paid down"))
                .andExpect(jsonPath("$.revision").value(1));

        mockMvc.perform(get("/api/households/{h}/liabilities/{l}", householdId, liabilityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outstandingBalance").value(300.00))
                .andExpect(jsonPath("$.balanceAsOf").value("2026-02-01"))
                .andExpect(jsonPath("$.revision").value(1));
    }

    @Test
    void newBalanceHistoryEndpointReturnsEmptyBeforeAnyUpdate() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        String liabilityId = createLiabilityId(householdId, "Loan", "PERSONAL_LOAN", "500.00", "PHP",
                LocalDate.now().toString());

        mockMvc.perform(get("/api/households/{h}/liabilities/{l}/balances", householdId, liabilityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listsBalanceHistoryInDeterministicRevisionOrder() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        String liabilityId = createLiabilityId(householdId, "Loan", "PERSONAL_LOAN", "500.00", "PHP",
                "2026-01-01");

        recordBalance(householdId, liabilityId, "400.00", "2026-02-01", "First payment", 0)
                .andExpect(status().isCreated());
        recordBalance(householdId, liabilityId, "250.00", "2026-03-01", "Second payment", 1)
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/households/{h}/liabilities/{l}/balances", householdId, liabilityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].revision").value(1))
                .andExpect(jsonPath("$[0].previousBalance").value("500.00"))
                .andExpect(jsonPath("$[0].newBalance").value("400.00"))
                .andExpect(jsonPath("$[1].revision").value(2))
                .andExpect(jsonPath("$[1].previousBalance").value("400.00"))
                .andExpect(jsonPath("$[1].newBalance").value("250.00"));
    }

    @Test
    void acceptsBalanceIncreaseAndZeroBalance() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        String liabilityId = createLiabilityId(householdId, "Loan", "PERSONAL_LOAN", "500.00", "PHP",
                "2026-01-01");

        recordBalance(householdId, liabilityId, "600.00", "2026-02-01", "New charge", 0)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.newBalance").value("600.00"));
        recordBalance(householdId, liabilityId, "0.00", "2026-03-01", "Paid off", 1)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.newBalance").value("0.00"));

        mockMvc.perform(get("/api/households/{h}/liabilities/{l}", householdId, liabilityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outstandingBalance").value(0));
    }

    @Test
    void rejectsStaleRevision() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        String liabilityId = createLiabilityId(householdId, "Loan", "PERSONAL_LOAN", "500.00", "PHP",
                "2026-01-01");

        recordBalance(householdId, liabilityId, "400.00", "2026-02-01", "First payment", 0)
                .andExpect(status().isCreated());

        recordBalance(householdId, liabilityId, "999.00", "2026-04-01", "Replay of stale revision", 0)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("LIABILITY_REVISION_CONFLICT"));

        mockMvc.perform(get("/api/households/{h}/liabilities/{l}/balances", householdId, liabilityId))
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/households/{h}/liabilities/{l}", householdId, liabilityId))
                .andExpect(jsonPath("$.outstandingBalance").value(400.00));
    }

    @Test
    void rejectsNegativeBalanceReplacement() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        String liabilityId = createLiabilityId(householdId, "Loan", "PERSONAL_LOAN", "500.00", "PHP",
                LocalDate.now().toString());

        recordBalance(householdId, liabilityId, "-1", LocalDate.now().toString(), "Reason", 0)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsBlankReason() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        String liabilityId = createLiabilityId(householdId, "Loan", "PERSONAL_LOAN", "500.00", "PHP",
                LocalDate.now().toString());

        recordBalance(householdId, liabilityId, "400.00", LocalDate.now().toString(), "   ", 0)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsMissingExpectedRevision() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        String liabilityId = createLiabilityId(householdId, "Loan", "PERSONAL_LOAN", "500.00", "PHP",
                LocalDate.now().toString());
        String body = objectMapper.writeValueAsString(new HashMap<>() {{
            put("outstandingBalance", "400.00");
            put("balanceAsOf", LocalDate.now().toString());
            put("reason", "Reason");
        }});

        mockMvc.perform(post("/api/households/{h}/liabilities/{l}/balances", householdId, liabilityId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsBalanceUpdateWithExcessiveFractionalScale() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        String liabilityId = createLiabilityId(householdId, "Loan", "PERSONAL_LOAN", "500.00", "PHP",
                LocalDate.now().toString());

        recordBalance(householdId, liabilityId, "100.005", LocalDate.now().toString(), "Reason", 0)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void returnsNotFoundForUnknownHouseholdOnBalanceUpdate() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        String liabilityId = createLiabilityId(householdId, "Loan", "PERSONAL_LOAN", "500.00", "PHP",
                LocalDate.now().toString());

        recordBalance(java.util.UUID.randomUUID().toString(), liabilityId, "400.00", LocalDate.now().toString(),
                "Reason", 0)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("HOUSEHOLD_NOT_FOUND"));
    }

    @Test
    void returnsNotFoundWhenLiabilityBelongsToAnotherHouseholdOnBalanceUpdate() throws Exception {
        String householdOneId = createHouseholdId("Household One", "PHP");
        String householdTwoId = createHouseholdId("Household Two", "PHP");
        String liabilityId = createLiabilityId(householdOneId, "Loan", "PERSONAL_LOAN", "500.00", "PHP",
                LocalDate.now().toString());

        recordBalance(householdTwoId, liabilityId, "400.00", LocalDate.now().toString(), "Reason", 0)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("LIABILITY_NOT_FOUND"));
    }

    @Test
    void concurrentSubmissionsFromSameRevisionProduceExactlyOneSuccessAndOneConflict() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        String liabilityId = createLiabilityId(householdId, "Loan", "PERSONAL_LOAN", "500.00", "PHP",
                "2026-01-01");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> submitA = () -> recordBalance(householdId, liabilityId, "100.00", "2026-02-01",
                    "Race A", 0).andReturn().getResponse().getStatus();
            Callable<Integer> submitB = () -> recordBalance(householdId, liabilityId, "200.00", "2026-02-01",
                    "Race B", 0).andReturn().getResponse().getStatus();

            Future<Integer> resultA = executor.submit(submitA);
            Future<Integer> resultB = executor.submit(submitB);
            List<Integer> statuses = List.of(resultA.get(30, TimeUnit.SECONDS), resultB.get(30, TimeUnit.SECONDS));

            assertThat(statuses).containsExactlyInAnyOrder(201, 409);
        } finally {
            executor.shutdown();
        }

        mockMvc.perform(get("/api/households/{h}/liabilities/{l}/balances", householdId, liabilityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void rollsBackBalanceReplacementWhenAuditAppendFailsAfterParentFlush() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        String liabilityId = createLiabilityId(householdId, "Loan", "PERSONAL_LOAN", "500.00", "PHP",
                "2026-01-01");

        // The service's own append targets revision 1 (0 -> 1 on this first
        // update). Pre-inserting a history row at that exact (liability,
        // revision) pair makes the unique constraint fail the append only
        // after replaceBalance()'s saveAndFlush has already applied revision 1
        // to the parent row, proving the whole transaction rolls back rather
        // than leaving the liability update committed with no audit trail.
        Liability liability = liabilityRepository.findById(UUID.fromString(liabilityId)).orElseThrow();
        liabilityBalanceHistoryRepository.saveAndFlush(new LiabilityBalanceHistory(
                liability, "PHP", new BigDecimal("500.00"), LocalDate.parse("2026-01-01"), SourceType.MANUAL_ENTRY,
                new BigDecimal("999.00"), LocalDate.parse("2026-01-01"), SourceType.MANUAL_ENTRY,
                "Pre-existing collision row", 1));

        assertThatThrownBy(() -> liabilityService.recordBalance(
                UUID.fromString(householdId), UUID.fromString(liabilityId), new BigDecimal("300.00"),
                LocalDate.parse("2026-02-01"), "Paid down", 0L))
                .isInstanceOf(RuntimeException.class);

        Liability afterFailure = liabilityRepository.findById(UUID.fromString(liabilityId)).orElseThrow();
        assertThat(afterFailure.getOutstandingBalance()).isEqualByComparingTo("500.00");
        assertThat(afterFailure.getBalanceAsOf()).isEqualTo(LocalDate.parse("2026-01-01"));
        assertThat(afterFailure.getRevision()).isEqualTo(0L);

        List<LiabilityBalanceHistory> historyRows = liabilityBalanceHistoryRepository
                .findByLiability_IdOrderByRevisionAsc(UUID.fromString(liabilityId));
        assertThat(historyRows).hasSize(1);
        assertThat(historyRows.get(0).getReason()).isEqualTo("Pre-existing collision row");
    }

    @Test
    void priorSnapshotUnaffectedByLaterBalanceReplacementAndFutureSnapshotReflectsIt() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        String liabilityId = createLiabilityId(householdId, "Loan", "PERSONAL_LOAN", "500.00", "PHP", "2026-01-01");

        String priorSnapshotId = createSnapshotId(householdId, "2026-01-15");
        mockMvc.perform(get("/api/households/{h}/financial-snapshots/{s}", householdId, priorSnapshotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liabilityLineItems[0].sourceLiabilityId").value(liabilityId))
                .andExpect(jsonPath("$.liabilityLineItems[0].value").value(500.00));

        recordBalance(householdId, liabilityId, "300.00", "2026-02-01", "Paid down", 0)
                .andExpect(status().isCreated());

        // The already-captured snapshot is a separate, immutable copy: it must
        // not move when the source liability's balance is later replaced.
        mockMvc.perform(get("/api/households/{h}/financial-snapshots/{s}", householdId, priorSnapshotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liabilityLineItems[0].value").value(500.00));

        // The balance replacement itself must not have created a snapshot.
        mockMvc.perform(get("/api/households/{h}/financial-snapshots", householdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // A snapshot captured after the replacement reflects it through the
        // existing current-source read, unchanged.
        String laterSnapshotId = createSnapshotId(householdId, "2026-02-15");
        mockMvc.perform(get("/api/households/{h}/financial-snapshots/{s}", householdId, laterSnapshotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liabilityLineItems[0].value").value(300.00));
    }

    @Test
    void preservesMaximumExactMonetaryValueOnBalanceReplacement() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        String liabilityId = createLiabilityId(householdId, "Loan", "PERSONAL_LOAN", "1.00", "PHP",
                LocalDate.now().toString());

        recordBalance(householdId, liabilityId, "99999999999999999.99", LocalDate.now().toString(),
                "Maximum exact value", 0)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.newBalance").value("99999999999999999.99"));

        mockMvc.perform(get("/api/households/{h}/liabilities/{l}/balances", householdId, liabilityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].newBalance").value("99999999999999999.99"));
    }

    @Test
    void preservesSameOlderAndNewerSuppliedDatesInRevisionOrder() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        String liabilityId = createLiabilityId(householdId, "Loan", "PERSONAL_LOAN", "500.00", "PHP",
                "2026-03-01");

        recordBalance(householdId, liabilityId, "400.00", "2026-01-01", "Older correction", 0)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.newBalanceAsOf").value("2026-01-01"));
        recordBalance(householdId, liabilityId, "350.00", "2026-01-01", "Same-date correction", 1)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.newBalanceAsOf").value("2026-01-01"));
        recordBalance(householdId, liabilityId, "300.00", "2026-05-01", "Newer correction", 2)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.newBalanceAsOf").value("2026-05-01"));

        mockMvc.perform(get("/api/households/{h}/liabilities/{l}/balances", householdId, liabilityId))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].revision").value(1))
                .andExpect(jsonPath("$[0].newBalanceAsOf").value("2026-01-01"))
                .andExpect(jsonPath("$[1].revision").value(2))
                .andExpect(jsonPath("$[1].newBalanceAsOf").value("2026-01-01"))
                .andExpect(jsonPath("$[2].revision").value(3))
                .andExpect(jsonPath("$[2].newBalanceAsOf").value("2026-05-01"));

        mockMvc.perform(get("/api/households/{h}/liabilities/{l}", householdId, liabilityId))
                .andExpect(jsonPath("$.balanceAsOf").value("2026-05-01"))
                .andExpect(jsonPath("$.outstandingBalance").value(300.00));
    }

    @Test
    void preservesLiabilityIdentityAcrossBalanceReplacement() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        String liabilityId = createLiabilityId(householdId, "Home Mortgage", "MORTGAGE", "500.00", "PHP",
                "2026-01-01");

        recordBalance(householdId, liabilityId, "400.00", "2026-02-01", "Paid down", 0)
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/households/{h}/liabilities/{l}", householdId, liabilityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(liabilityId))
                .andExpect(jsonPath("$.householdId").value(householdId))
                .andExpect(jsonPath("$.name").value("Home Mortgage"))
                .andExpect(jsonPath("$.liabilityType").value("MORTGAGE"))
                .andExpect(jsonPath("$.currency").value("PHP"))
                .andExpect(jsonPath("$.outstandingBalance").value(400.00))
                .andExpect(jsonPath("$.sourceType").value("MANUAL_ENTRY"));
    }

    @Test
    void returnsNotFoundForUnknownLiabilityOnBalanceUpdate() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");

        recordBalance(householdId, UUID.randomUUID().toString(), "400.00", LocalDate.now().toString(), "Reason", 0)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("LIABILITY_NOT_FOUND"));
    }

    @Test
    void returnsNotFoundWhenReadingBalanceHistoryForLiabilityInAnotherHousehold() throws Exception {
        String householdOneId = createHouseholdId("Household One", "PHP");
        String householdTwoId = createHouseholdId("Household Two", "PHP");
        String liabilityId = createLiabilityId(householdOneId, "Loan", "PERSONAL_LOAN", "500.00", "PHP",
                LocalDate.now().toString());

        mockMvc.perform(get("/api/households/{h}/liabilities/{l}/balances", householdTwoId, liabilityId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("LIABILITY_NOT_FOUND"));
    }

    @Test
    void rejectsReasonExceedingMaxLength() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        String liabilityId = createLiabilityId(householdId, "Loan", "PERSONAL_LOAN", "500.00", "PHP",
                LocalDate.now().toString());

        recordBalance(householdId, liabilityId, "400.00", LocalDate.now().toString(), "a".repeat(501), 0)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsFutureBalanceAsOfDateOnUpdate() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        String liabilityId = createLiabilityId(householdId, "Loan", "PERSONAL_LOAN", "500.00", "PHP",
                LocalDate.now().toString());

        recordBalance(householdId, liabilityId, "400.00", LocalDate.now().plusDays(1).toString(), "Reason", 0)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
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

    private String createLiabilityId(
            String householdId, String name, String liabilityType, String outstandingBalance, String currency,
            String balanceAsOf
    ) throws Exception {
        String body = objectMapper.writeValueAsString(new HashMap<>() {{
            put("name", name);
            put("liabilityType", liabilityType);
            put("outstandingBalance", outstandingBalance);
            put("currency", currency);
            put("balanceAsOf", balanceAsOf);
        }});
        String response = mockMvc.perform(post("/api/households/{h}/liabilities", householdId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private String createSnapshotId(String householdId, String asOfDate) throws Exception {
        String body = objectMapper.writeValueAsString(new HashMap<>() {{
            put("asOfDate", asOfDate);
        }});
        String response = mockMvc.perform(post("/api/households/{h}/financial-snapshots", householdId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private ResultActions recordBalance(
            String householdId, String liabilityId, String outstandingBalance, String balanceAsOf, String reason,
            long expectedRevision
    ) throws Exception {
        String body = objectMapper.writeValueAsString(new HashMap<>() {{
            put("outstandingBalance", outstandingBalance);
            put("balanceAsOf", balanceAsOf);
            put("reason", reason);
            put("expectedRevision", expectedRevision);
        }});
        return mockMvc.perform(post("/api/households/{h}/liabilities/{l}/balances", householdId, liabilityId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
