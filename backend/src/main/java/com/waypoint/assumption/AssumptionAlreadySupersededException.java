package com.waypoint.assumption;

import java.util.UUID;

public class AssumptionAlreadySupersededException extends RuntimeException {

    public AssumptionAlreadySupersededException(UUID assumptionId) {
        super("Planning assumption is already superseded: " + assumptionId);
    }
}
