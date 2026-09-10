package com.waypoint.assumption;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlanningAssumptionRepository extends JpaRepository<PlanningAssumption, UUID> {

    Optional<PlanningAssumption> findByIdAndHousehold_Id(UUID id, UUID householdId);

    List<PlanningAssumption> findByHousehold_IdOrderByNameAscCreatedAtAscIdAsc(UUID householdId);

    @Query("""
            SELECT a FROM PlanningAssumption a
            WHERE a.household.id = :householdId
              AND a.supersededBy IS NULL
              AND a.effectiveFrom <= :asOf
              AND (a.effectiveUntil IS NULL OR a.effectiveUntil >= :asOf)
            ORDER BY a.name ASC, a.createdAt ASC, a.id ASC
            """)
    List<PlanningAssumption> findActiveAsOf(@Param("householdId") UUID householdId, @Param("asOf") LocalDate asOf);

    /**
     * Atomically claims the supersession link at the database transaction
     * boundary: the {@code WHERE superseded_by_id IS NULL} predicate is
     * re-evaluated by PostgreSQL under the row lock this UPDATE acquires, so
     * when two overlapping transactions race to supersede the same prior
     * version, the loser's update matches zero rows instead of silently
     * overwriting the winner's link. {@code flushAutomatically} ensures the
     * replacement row (referenced by the foreign key) is already visible to
     * this statement; {@code clearAutomatically} keeps the persistence
     * context from holding a stale, unlocked copy of the prior version
     * afterward.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            UPDATE planning_assumptions
            SET superseded_by_id = :replacementId
            WHERE id = :priorId AND superseded_by_id IS NULL
            """, nativeQuery = true)
    int linkSupersessionIfNotAlreadySuperseded(
            @Param("priorId") UUID priorId,
            @Param("replacementId") UUID replacementId
    );
}
