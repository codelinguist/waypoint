package com.waypoint.household;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ObligationRepository extends JpaRepository<Obligation, UUID> {

    List<Obligation> findByHousehold_IdOrderByCreatedAtAscIdAsc(UUID householdId);

    Optional<Obligation> findByIdAndHousehold_Id(UUID id, UUID householdId);
}
