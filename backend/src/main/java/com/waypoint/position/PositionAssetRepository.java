package com.waypoint.position;

import com.waypoint.household.Asset;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Feature-local, read-only view over the shared {@link Asset} entity. Orders by
 * {@code id} (not {@code createdAt}) to satisfy this endpoint's deterministic
 * UUID-ordering contract, distinct from the household module's own
 * {@code AssetRepository} creation-order listing.
 */
public interface PositionAssetRepository extends JpaRepository<Asset, UUID> {

    List<Asset> findByHousehold_IdOrderByIdAsc(UUID householdId);
}
