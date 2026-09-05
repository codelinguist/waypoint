package com.waypoint.household;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialGoalRepository extends JpaRepository<FinancialGoal, UUID> {

    List<FinancialGoal> findByHousehold_IdOrderByPriorityAscCreatedAtAscIdAsc(UUID householdId);

    Optional<FinancialGoal> findByIdAndHousehold_Id(UUID id, UUID householdId);
}
