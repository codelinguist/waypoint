package com.waypoint.household;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetValuationRepository extends JpaRepository<AssetValuation, UUID> {

    List<AssetValuation> findByAsset_IdAndAsset_Household_IdOrderByRevisionAsc(UUID assetId, UUID householdId);
}
