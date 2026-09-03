package com.waypoint.household;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LiabilityRepository extends JpaRepository<Liability, UUID> {

    List<Liability> findByHousehold_IdOrderByCreatedAtAscIdAsc(UUID householdId);

    Optional<Liability> findByIdAndHousehold_Id(UUID id, UUID householdId);
}
