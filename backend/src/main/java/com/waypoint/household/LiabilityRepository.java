package com.waypoint.household;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LiabilityRepository extends JpaRepository<Liability, UUID> {

    List<Liability> findByHousehold_IdOrderByCreatedAtAscIdAsc(UUID householdId);

    Optional<Liability> findByIdAndHousehold_Id(UUID id, UUID householdId);

    boolean existsByIdAndHousehold_Id(UUID id, UUID householdId);

    /**
     * Conditionally replaces the outstanding balance/date and advances
     * {@code revision} by exactly one, in a single atomic statement gated by
     * the {@code revision = :expectedRevision} clause. Returns the number of
     * rows updated: {@code 1} on success, {@code 0} when the row's current
     * revision no longer matches — a caller-stale revision and a genuinely
     * concurrent conflicting write both surface identically this way. Doing
     * this as a direct bulk update, rather than mutating the loaded entity
     * and relying on JPA dirty checking, also means a resubmission whose
     * values are textually identical to the current row still counts as a
     * distinct accepted change instead of being silently skipped (see D021).
     * A bulk update bypasses Hibernate's {@code @UpdateTimestamp} lifecycle
     * handling, so {@code updatedAt} is set explicitly here rather than left
     * to that annotation.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Liability l
            set l.outstandingBalance = :outstandingBalance,
                l.balanceAsOf = :balanceAsOf,
                l.sourceType = com.waypoint.household.SourceType.MANUAL_ENTRY,
                l.updatedAt = :updatedAt,
                l.revision = l.revision + 1
            where l.id = :liabilityId
              and l.household.id = :householdId
              and l.revision = :expectedRevision
            """)
    int applyBalance(
            @Param("liabilityId") UUID liabilityId,
            @Param("householdId") UUID householdId,
            @Param("outstandingBalance") BigDecimal outstandingBalance,
            @Param("balanceAsOf") LocalDate balanceAsOf,
            @Param("updatedAt") Instant updatedAt,
            @Param("expectedRevision") long expectedRevision
    );
}
