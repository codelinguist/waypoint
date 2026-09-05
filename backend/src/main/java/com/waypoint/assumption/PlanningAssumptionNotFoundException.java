package com.waypoint.assumption;

import java.util.UUID;

public class PlanningAssumptionNotFoundException extends RuntimeException {

    public PlanningAssumptionNotFoundException(UUID assumptionId) {
        super("Planning assumption not found: " + assumptionId);
    }
}
