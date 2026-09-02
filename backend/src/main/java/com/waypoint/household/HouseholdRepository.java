package com.waypoint.household;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HouseholdRepository extends JpaRepository<Household, UUID> {
}
