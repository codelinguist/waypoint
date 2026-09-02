package com.waypoint.household;

import java.util.UUID;

public class HouseholdNotFoundException extends RuntimeException {

    public HouseholdNotFoundException(UUID householdId) {
        super("Household not found: " + householdId);
    }
}
