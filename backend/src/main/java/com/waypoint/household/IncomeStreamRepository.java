package com.waypoint.household;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncomeStreamRepository extends JpaRepository<IncomeStream, UUID> {

    List<IncomeStream> findByHousehold_IdOrderByCreatedAtAscIdAsc(UUID householdId);

    Optional<IncomeStream> findByIdAndHousehold_Id(UUID id, UUID householdId);
}
