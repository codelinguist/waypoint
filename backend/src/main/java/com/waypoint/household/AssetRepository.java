package com.waypoint.household;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetRepository extends JpaRepository<Asset, UUID> {

    List<Asset> findByHousehold_IdOrderByCreatedAtAscIdAsc(UUID householdId);

    Optional<Asset> findByIdAndHousehold_Id(UUID id, UUID householdId);
}
