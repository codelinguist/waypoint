package com.waypoint.position;

import com.waypoint.household.Liability;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Feature-local, read-only view over the shared {@link Liability} entity. Orders
 * by {@code id} (not {@code createdAt}) to satisfy this endpoint's deterministic
 * UUID-ordering contract, distinct from the household module's own
 * {@code LiabilityRepository} creation-order listing.
 */
public interface PositionLiabilityRepository extends JpaRepository<Liability, UUID> {

    List<Liability> findByHousehold_IdOrderByIdAsc(UUID householdId);
}
