package com.waypoint.household;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialSnapshotRepository extends JpaRepository<FinancialSnapshot, UUID> {

    List<FinancialSnapshot> findByHousehold_IdOrderByAsOfDateAscCapturedAtAscIdAsc(UUID householdId);

    Optional<FinancialSnapshot> findByIdAndHousehold_Id(UUID id, UUID householdId);
}
