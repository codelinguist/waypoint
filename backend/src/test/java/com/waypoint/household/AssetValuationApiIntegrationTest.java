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
import java.util.concurrent.CyclicBarrier;
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
 * Exercises the asset-valuation-update and history API (WAP-16) against a
 * real PostgreSQL instance, including the optimistic-revision concurrency
 * and atomic-commit behavior that a mocked repository cannot demonstrate.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AssetValuationApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private HouseholdRepository householdRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private AssetValuationRepository assetValuationRepository;

    @Test
    void retrievesCurrentValuationWithZeroRevisionBeforeAnyUpdate() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        String assetId = createAsset(householdId, "1000.00", "900.00", LocalDate.of(2026, 1, 1).toString());

        getCurrentValuation(householdId, assetId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assetId").value(assetId))
                .andExpect(jsonPath("$.householdId").value(householdId))
                .andExpect(jsonPath("$.estimatedValue").value("1000.00"))
                .andExpect(jsonPath("$.planningValue").value("900.00"))
                .andExpect(jsonPath("$.valuedAt").value("2026-01-01"))
                .andExpect(jsonPath("$.sourceType").value("MANUAL_ENTRY"))
                .andExpect(jsonPath("$.revision").value(0));

        getValuationHistory(householdId, assetId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void updatesValuationAndRecordsExactBeforeAfterHistory() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        String assetId = createAsset(householdId, "1000.00", "900.00", LocalDate.of(2026, 1, 1).toString());

        updateValuation(householdId, assetId, "1234.56", "1111.11", "2026-02-15", "Independent appraisal", 0)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estimatedValue").value("1234.56"))
                .andExpect(jsonPath("$.planningValue").value("1111.11"))
                .andExpect(jsonPath("$.valuedAt").value("2026-02-15"))
                .andExpect(jsonPath("$.revision").value(1));

        getValuationHistory(householdId, assetId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].assetId").value(assetId))
                .andExpect(jsonPath("$[0].householdId").value(householdId))
                .andExpect(jsonPath("$[0].revision").value(1))
                .andExpect(jsonPath("$[0].previousEstimatedValue").value("1000.00"))
                .andExpect(jsonPath("$[0].previousPlanningValue").value("900.00"))
                .andExpect(jsonPath("$[0].previousValuedAt").value("2026-01-01"))
                .andExpect(jsonPath("$[0].previousSourceType").value("MANUAL_ENTRY"))
                .andExpect(jsonPath("$[0].newEstimatedValue").value("1234.56"))
                .andExpect(jsonPath("$[0].newPlanningValue").value("1111.11"))
                .andExpect(jsonPath("$[0].newValuedAt").value("2026-02-15"))
                .andExpect(jsonPath("$[0].newSourceType").value("MANUAL_ENTRY"))
                .andExpect(jsonPath("$[0].reason").value("Independent appraisal"))
                .andExpect(jsonPath("$[0].recordedAt").exists());
    }

    @Test
    void rejectsUpdateWithStaleRevisionAndLeavesAssetAndHistoryUnchanged() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        String assetId = createAsset(householdId, "1000.00", "900.00", LocalDate.of(2026, 1, 1).toString());

        updateValuation(householdId, assetId, "1100.00", "1000.00", "2026-02-01", "First update", 0)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revision").value(1));

        // Repeating with the now-stale (already-consumed) revision must not append another change.
        updateValuation(householdId, assetId, "9999.00", "9999.00", "2026-03-01", "Replay attempt", 0)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ASSET_REVISION_CONFLICT"));

        getCurrentValuation(householdId, assetId)
                .andExpect(jsonPath("$.estimatedValue").value("1100.00"))
                .andExpect(jsonPath("$.revision").value(1));
        getValuationHistory(householdId, assetId)
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void rejectsUpdateWhenPlanningValueExceedsEstimatedValueAndAddsNoHistory() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        String assetId = createAsset(householdId, "1000.00", "900.00", LocalDate.of(2026, 1, 1).toString());

        updateValuation(householdId, assetId, "100.00", "150.00", "2026-02-01", "Bad update", 0)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));

        getCurrentValuation(householdId, assetId)
                .andExpect(jsonPath("$.estimatedValue").value("1000.00"))
                .andExpect(jsonPath("$.revision").value(0));
        getValuationHistory(householdId, assetId).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void rejectsUpdateWithBlankReason() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        String assetId = createAsset(householdId, "1000.00", "900.00", LocalDate.of(2026, 1, 1).toString());

        updateValuation(householdId, assetId, "1100.00", "1000.00", "2026-02-01", "   ", 0)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsUpdateWithExcessiveFractionalScaleInsteadOfRounding() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        String assetId = createAsset(householdId, "1000.00", "900.00", LocalDate.of(2026, 1, 1).toString());

        updateValuation(householdId, assetId, "1100.005", "1000.00", "2026-02-01", "Too precise", 0)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsUpdateForUnknownHousehold() throws Exception {
        updateValuation(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                "100.00", "100.00", "2026-01-01", "reason", 0)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("HOUSEHOLD_NOT_FOUND"));
    }

    @Test
    void returnsNotFoundWhenUpdatingValuationForAssetInAnotherHousehold() throws Exception {
        String householdOneId = createHouseholdId("Household One", "PHP");
        String householdTwoId = createHouseholdId("Household Two", "PHP");
        String assetId = createAsset(householdOneId, "1000.00", "900.00", LocalDate.of(2026, 1, 1).toString());

        updateValuation(householdTwoId, assetId, "100.00", "100.00", "2026-01-01", "reason", 0)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ASSET_NOT_FOUND"));
    }

    @Test
    void returnsNotFoundWhenReadingCurrentValuationForAssetInAnotherHousehold() throws Exception {
        String householdOneId = createHouseholdId("Household One", "PHP");
        String householdTwoId = createHouseholdId("Household Two", "PHP");
        String assetId = createAsset(householdOneId, "1000.00", "900.00", LocalDate.of(2026, 1, 1).toString());

        getCurrentValuation(householdTwoId, assetId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ASSET_NOT_FOUND"));
    }

    @Test
    void returnsNotFoundWhenReadingValuationHistoryForUnknownHousehold() throws Exception {
        getValuationHistory(UUID.randomUUID().toString(), UUID.randomUUID().toString())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("HOUSEHOLD_NOT_FOUND"));
    }

    @Test
    void ordersValuationHistoryByRevisionAcrossMultipleUpdates() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        String assetId = createAsset(householdId, "1000.00", "900.00", LocalDate.of(2026, 1, 1).toString());

        updateValuation(householdId, assetId, "1100.00", "1000.00", "2026-02-01", "First", 0)
                .andExpect(status().isOk());
        updateValuation(householdId, assetId, "900.00", "800.00", "2026-01-15", "Correction to an earlier date", 1)
                .andExpect(status().isOk());
        updateValuation(householdId, assetId, "900.00", "800.00", "2026-01-15", "Same-date confirmation", 2)
                .andExpect(status().isOk());

        getValuationHistory(householdId, assetId)
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].revision").value(1))
                .andExpect(jsonPath("$[0].newEstimatedValue").value("1100.00"))
                .andExpect(jsonPath("$[1].revision").value(2))
                .andExpect(jsonPath("$[1].previousEstimatedValue").value("1100.00"))
                .andExpect(jsonPath("$[1].newValuedAt").value("2026-01-15"))
                .andExpect(jsonPath("$[2].revision").value(3))
                .andExpect(jsonPath("$[2].previousValuedAt").value("2026-01-15"))
                .andExpect(jsonPath("$[2].newValuedAt").value("2026-01-15"));

        getCurrentValuation(householdId, assetId)
                .andExpect(jsonPath("$.revision").value(3))
                .andExpect(jsonPath("$.estimatedValue").value("900.00"));
    }

    @Test
    void concurrentUpdatesBasedOnSameRevisionYieldExactlyOneSuccessAndOneStructuredConflict() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        String assetId = createAsset(householdId, "1000.00", "900.00", LocalDate.of(2026, 1, 1).toString());

        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        int statusA;
        int statusB;
        try {
            Callable<Integer> submitA = () -> {
                barrier.await(10, TimeUnit.SECONDS);
                return updateValuation(householdId, assetId, "2000.00", "1900.00", "2026-03-01", "Contender A", 0)
                        .andReturn().getResponse().getStatus();
            };
            Callable<Integer> submitB = () -> {
                barrier.await(10, TimeUnit.SECONDS);
                return updateValuation(householdId, assetId, "3000.00", "2900.00", "2026-03-01", "Contender B", 0)
                        .andReturn().getResponse().getStatus();
            };
            Future<Integer> futureA = executor.submit(submitA);
            Future<Integer> futureB = executor.submit(submitB);
            statusA = futureA.get(15, TimeUnit.SECONDS);
            statusB = futureB.get(15, TimeUnit.SECONDS);
        } finally {
            executor.shutdown();
        }

        List<Integer> statuses = List.of(statusA, statusB);
        assertThat(statuses).containsExactlyInAnyOrder(200, 409);

        // Exactly one accepted change committed: one history row, and current state matches only that winner.
        getValuationHistory(householdId, assetId).andExpect(jsonPath("$.length()").value(1));
        getCurrentValuation(householdId, assetId).andExpect(jsonPath("$.revision").value(1));
    }

    @Test
    void existingSnapshotIsUnaffectedByALaterValuationUpdate() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        String assetId = createAsset(householdId, "1000.00", "900.00", LocalDate.of(2026, 1, 1).toString());

        String snapshotBody = mockMvc.perform(post("/api/households/{h}/financial-snapshots", householdId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new HashMap<>() {{
                            put("asOfDate", "2026-01-10");
                        }})))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assetLineItems[0].value").value(900.00))
                .andReturn().getResponse().getContentAsString();
        String snapshotId = objectMapper.readTree(snapshotBody).get("id").asText();

        updateValuation(householdId, assetId, "1200.00", "1150.00", "2026-02-01", "Post-snapshot update", 0)
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/households/{h}/financial-snapshots/{s}", householdId, snapshotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assetLineItems[0].value").value(900.00));
    }

    @Test
    void updateAdvancesUpdatedAtWhilePreservingIdentityAndOtherMetadata() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        String assetId = createAsset(householdId, "1000.00", "900.00", LocalDate.of(2026, 1, 1).toString());

        String beforeBody = mockMvc.perform(get("/api/households/{h}/assets/{a}", householdId, assetId))
                .andReturn().getResponse().getContentAsString();
        var before = objectMapper.readTree(beforeBody);

        Thread.sleep(5);
        updateValuation(householdId, assetId, "1234.56", "1111.11", "2026-02-15", "Independent appraisal", 0)
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/households/{h}/assets/{a}", householdId, assetId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(before.get("id").asText()))
                .andExpect(jsonPath("$.householdId").value(before.get("householdId").asText()))
                .andExpect(jsonPath("$.name").value(before.get("name").asText()))
                .andExpect(jsonPath("$.assetType").value(before.get("assetType").asText()))
                .andExpect(jsonPath("$.currency").value(before.get("currency").asText()))
                .andExpect(jsonPath("$.liquidity").value(before.get("liquidity").asText()))
                .andExpect(jsonPath("$.createdAt").value(before.get("createdAt").asText()))
                .andExpect(result -> {
                    String afterBody = result.getResponse().getContentAsString();
                    String updatedAt = objectMapper.readTree(afterBody).get("updatedAt").asText();
                    assertThat(updatedAt).isNotEqualTo(before.get("updatedAt").asText());
                });
    }

    @Test
    void auditWriteFailureRollsBackTheAssetRowUpdate() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        String assetId = createAsset(householdId, "1000.00", "900.00", LocalDate.of(2026, 1, 1).toString());

        // Pre-insert an audit row already occupying the (asset_id, revision) the next
        // accepted update would produce, so the append after the asset row's UPDATE
        // hits the unique constraint and the whole transaction must roll back.
        Asset asset = assetRepository.findByIdAndHousehold_Id(UUID.fromString(assetId), UUID.fromString(householdId))
                .orElseThrow();
        Household household = householdRepository.findById(UUID.fromString(householdId)).orElseThrow();
        assetValuationRepository.save(new AssetValuation(
                asset,
                household,
                1L,
                asset.getEstimatedValue(),
                asset.getPlanningValue(),
                asset.getValuedAt(),
                asset.getSourceType(),
                new BigDecimal("1.00"),
                new BigDecimal("1.00"),
                LocalDate.of(2026, 1, 2),
                SourceType.MANUAL_ENTRY,
                "Pre-existing colliding revision"
        ));

        assertThatThrownBy(() ->
                updateValuation(householdId, assetId, "1234.56", "1111.11", "2026-02-15", "Should roll back", 0))
                .isInstanceOf(Exception.class);

        getCurrentValuation(householdId, assetId)
                .andExpect(jsonPath("$.estimatedValue").value("1000.00"))
                .andExpect(jsonPath("$.planningValue").value("900.00"))
                .andExpect(jsonPath("$.valuedAt").value("2026-01-01"))
                .andExpect(jsonPath("$.revision").value(0));
        getValuationHistory(householdId, assetId)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].reason").value("Pre-existing colliding revision"));
    }

    @Test
    void acceptsMaximumPrecisionBoundaryAndZeroValues() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        String assetId = createAsset(householdId, "1000.00", "900.00", LocalDate.of(2026, 1, 1).toString());

        String maxValue = "9".repeat(17) + ".99";
        updateValuation(householdId, assetId, maxValue, maxValue, "2026-02-01", "Maximum precision boundary", 0)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estimatedValue").value(maxValue))
                .andExpect(jsonPath("$.planningValue").value(maxValue));

        updateValuation(householdId, assetId, "0.00", "0.00", "2026-02-02", "Zero valuation", 1)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estimatedValue").value("0.00"))
                .andExpect(jsonPath("$.planningValue").value("0.00"));
    }

    @Test
    void rejectsValueExceedingSeventeenIntegerDigits() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        String assetId = createAsset(householdId, "1000.00", "900.00", LocalDate.of(2026, 1, 1).toString());

        String tooLarge = "9".repeat(18) + ".00";
        updateValuation(householdId, assetId, tooLarge, "900.00", "2026-02-01", "Overflow attempt", 0)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));

        getCurrentValuation(householdId, assetId).andExpect(jsonPath("$.revision").value(0));
    }

    @Test
    void rejectsUpdateWithMissingValuedAt() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        String assetId = createAsset(householdId, "1000.00", "900.00", LocalDate.of(2026, 1, 1).toString());

        String body = objectMapper.writeValueAsString(new HashMap<>() {{
            put("estimatedValue", "1100.00");
            put("planningValue", "1000.00");
            put("reason", "Missing date");
            put("expectedRevision", 0);
        }});
        mockMvc.perform(post("/api/households/{h}/assets/{a}/valuations", householdId, assetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));

        getCurrentValuation(householdId, assetId).andExpect(jsonPath("$.revision").value(0));
    }

    @Test
    void rejectsUpdateWithFutureValuedAt() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        String assetId = createAsset(householdId, "1000.00", "900.00", LocalDate.of(2026, 1, 1).toString());

        String futureDate = LocalDate.now().plusYears(1).toString();
        updateValuation(householdId, assetId, "1100.00", "1000.00", futureDate, "Future date", 0)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));

        getCurrentValuation(householdId, assetId).andExpect(jsonPath("$.revision").value(0));
    }

    @Test
    void rejectsUpdateWithOverlongReason() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        String assetId = createAsset(householdId, "1000.00", "900.00", LocalDate.of(2026, 1, 1).toString());

        String overlongReason = "x".repeat(501);
        updateValuation(householdId, assetId, "1100.00", "1000.00", "2026-02-01", overlongReason, 0)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));

        getCurrentValuation(householdId, assetId).andExpect(jsonPath("$.revision").value(0));
    }

    @Test
    void newSnapshotAndCurrentPositionAfterValuationUpdateUseTheUpdatedValue() throws Exception {
        String householdId = createHouseholdId("Ralph Household", "PHP");
        String assetId = createAsset(householdId, "1000.00", "900.00", LocalDate.of(2026, 1, 1).toString());

        updateValuation(householdId, assetId, "1200.00", "1150.00", "2026-02-01", "Pre-snapshot update", 0)
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/households/{h}/financial-snapshots", householdId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new HashMap<>() {{
                            put("asOfDate", "2026-02-10");
                        }})))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assetLineItems[0].value").value(1150.00));

        mockMvc.perform(get("/api/households/{h}/financial-position", householdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assets[0].estimatedValue").value("1200.00"));
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

    private String createAsset(
            String householdId, String estimatedValue, String planningValue, String valuedAt
    ) throws Exception {
        String body = objectMapper.writeValueAsString(new HashMap<>() {{
            put("name", "Fund");
            put("assetType", "CASH");
            put("estimatedValue", estimatedValue);
            put("planningValue", planningValue);
            put("currency", "PHP");
            put("valuedAt", valuedAt);
            put("liquidity", "LIQUID");
        }});
        String response = mockMvc.perform(post("/api/households/{h}/assets", householdId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private ResultActions updateValuation(
            String householdId, String assetId, String estimatedValue, String planningValue,
            String valuedAt, String reason, long expectedRevision
    ) throws Exception {
        String body = objectMapper.writeValueAsString(new HashMap<>() {{
            put("estimatedValue", estimatedValue);
            put("planningValue", planningValue);
            put("valuedAt", valuedAt);
            put("reason", reason);
            put("expectedRevision", expectedRevision);
        }});
        return mockMvc.perform(post("/api/households/{h}/assets/{a}/valuations", householdId, assetId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions getCurrentValuation(String householdId, String assetId) throws Exception {
        return mockMvc.perform(get("/api/households/{h}/assets/{a}/valuations", householdId, assetId));
    }

    private ResultActions getValuationHistory(String householdId, String assetId) throws Exception {
        return mockMvc.perform(get("/api/households/{h}/assets/{a}/valuations/history", householdId, assetId));
    }
}
