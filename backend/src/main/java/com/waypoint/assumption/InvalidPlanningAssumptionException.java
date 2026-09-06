package com.waypoint.assumption;

/**
 * Thrown when a planning assumption request violates this aggregate's own
 * invariants (date ordering, supersession eligibility, name continuity)
 * independent of transport-layer bean validation.
 */
public class InvalidPlanningAssumptionException extends RuntimeException {

    public InvalidPlanningAssumptionException(String message) {
        super(message);
    }
}
