package com.waypoint.household;

import java.util.UUID;

public class LiabilityNotFoundException extends RuntimeException {

    public LiabilityNotFoundException(UUID liabilityId) {
        super("Liability not found: " + liabilityId);
    }
}
