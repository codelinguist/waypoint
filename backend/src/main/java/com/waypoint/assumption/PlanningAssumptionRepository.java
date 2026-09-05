package com.waypoint.assumption;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanningAssumptionRepository extends JpaRepository<PlanningAssumption, UUID> {

    List<PlanningAssumption> findByHousehold_IdOrderByNameAscEffectiveFromAscCreatedAtAscIdAsc(UUID householdId);

    Optional<PlanningAssumption> findByIdAndHousehold_Id(UUID id, UUID householdId);
}
