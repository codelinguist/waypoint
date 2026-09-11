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

public interface AssetRepository extends JpaRepository<Asset, UUID> {

    List<Asset> findByHousehold_IdOrderByCreatedAtAscIdAsc(UUID householdId);

    Optional<Asset> findByIdAndHousehold_Id(UUID id, UUID householdId);

    /**
     * Conditionally replaces the recorded valuation and advances
     * {@code revision} by exactly one, in a single atomic statement gated by
     * the {@code revision = :expectedRevision} clause. Returns the number of
     * rows updated: {@code 1} on success, {@code 0} when the row's current
     * revision no longer matches — a caller-stale revision and a genuinely
     * concurrent conflicting write both surface identically this way, and
     * PostgreSQL's row locking under a concurrent UPDATE guarantees exactly
     * one of two simultaneous callers on the same starting revision wins.
     * Doing this as a direct bulk update, rather than mutating the loaded
     * entity and relying on JPA dirty checking, also means a resubmission
     * whose values are textually identical to the current row still counts
     * as a distinct accepted change instead of being silently skipped. A
     * bulk update bypasses Hibernate's {@code @UpdateTimestamp} lifecycle
     * handling, so {@code updatedAt} is set explicitly here rather than left
     * to that annotation.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Asset a
            set a.estimatedValue = :estimatedValue,
                a.planningValue = :planningValue,
                a.valuedAt = :valuedAt,
                a.updatedAt = :updatedAt,
                a.revision = a.revision + 1
            where a.id = :assetId
              and a.household.id = :householdId
              and a.revision = :expectedRevision
            """)
    int applyValuation(
            @Param("assetId") UUID assetId,
            @Param("householdId") UUID householdId,
            @Param("estimatedValue") BigDecimal estimatedValue,
            @Param("planningValue") BigDecimal planningValue,
            @Param("valuedAt") LocalDate valuedAt,
            @Param("updatedAt") Instant updatedAt,
            @Param("expectedRevision") long expectedRevision
    );
}
