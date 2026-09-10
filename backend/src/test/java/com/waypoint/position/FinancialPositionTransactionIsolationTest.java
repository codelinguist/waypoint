package com.waypoint.position;

import static org.assertj.core.api.Assertions.assertThat;

import com.waypoint.household.Household;
import com.waypoint.household.HouseholdService;
import com.waypoint.household.Liability;
import com.waypoint.household.LiabilityService;
import com.waypoint.household.LiabilityType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Documents and proves the read transaction's consistency boundary: the
 * product brief requires that returned totals are derived from exactly the
 * returned rows, and that concurrent writes cannot mix different database read
 * views into one response.
 *
 * <p>{@link #serviceUsesReadOnlyRepeatableReadTransaction()} pins the exact
 * isolation contract {@link FinancialPositionService} relies on, so a future
 * change silently weakening it (e.g. to the READ COMMITTED default) fails this
 * test rather than only showing up as a rare, hard-to-reproduce read anomaly.
 *
 * <p>{@link #repeatableReadTransactionDoesNotSeeALiabilityCommittedMidRead()}
 * proves that isolation level actually holds a coherent snapshot against this
 * feature's own repository queries: a second thread commits a new liability
 * between this transaction's asset read and its liability read, and the
 * liability read still does not see it.
 */
@SpringBootTest
@Testcontainers
class FinancialPositionTransactionIsolationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private HouseholdService householdService;

    @Autowired
    private LiabilityService liabilityService;

    @Autowired
    private PositionAssetRepository assetRepository;

    @Autowired
    private PositionLiabilityRepository liabilityRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void serviceUsesReadOnlyRepeatableReadTransaction() {
        Transactional transactional = FinancialPositionService.class.getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.isolation()).isEqualTo(Isolation.REPEATABLE_READ);
        assertThat(transactional.readOnly()).isTrue();
    }

    @Test
    void repeatableReadTransactionDoesNotSeeALiabilityCommittedMidRead() throws InterruptedException {
        Household household = householdService.createHousehold("Isolation Test Household", "PHP");
        UUID householdId = household.getId();

        CountDownLatch assetsRead = new CountDownLatch(1);
        CountDownLatch liabilityCommitted = new CountDownLatch(1);
        AtomicReference<List<Liability>> liabilitiesSeenByReader = new AtomicReference<>();

        TransactionTemplate readTemplate = new TransactionTemplate(transactionManager);
        readTemplate.setIsolationLevel(TransactionTemplate.ISOLATION_REPEATABLE_READ);
        readTemplate.setReadOnly(true);

        Thread reader = new Thread(() -> readTemplate.executeWithoutResult(status -> {
            assetRepository.findByHousehold_IdOrderByIdAsc(householdId);
            assetsRead.countDown();
            await(liabilityCommitted);
            liabilitiesSeenByReader.set(liabilityRepository.findByHousehold_IdOrderByIdAsc(householdId));
        }));

        reader.start();
        await(assetsRead);
        liabilityService.createLiability(
                householdId, "Committed Mid-Read", LiabilityType.OTHER, new BigDecimal("1.00"), "PHP",
                LocalDate.now());
        liabilityCommitted.countDown();
        reader.join();

        assertThat(liabilitiesSeenByReader.get()).isEmpty();
        // The write is visible to a fresh transaction started after it committed,
        // proving the empty result above came from snapshot isolation, not a bug.
        assertThat(liabilityRepository.findByHousehold_IdOrderByIdAsc(householdId)).hasSize(1);
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
