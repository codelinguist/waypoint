package com.waypoint.household;

import java.util.UUID;

public class ObligationNotFoundException extends RuntimeException {

    public ObligationNotFoundException(UUID obligationId) {
        super("Obligation not found: " + obligationId);
    }
}
