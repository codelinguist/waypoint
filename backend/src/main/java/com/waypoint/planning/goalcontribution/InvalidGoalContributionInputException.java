package com.waypoint.planning.goalcontribution;

/**
 * Thrown when {@link GoalContributionCalculator} is invoked with inputs that
 * violate this calculation's own invariants, independent of any transport
 * (HTTP) validation that may have already run.
 */
public class InvalidGoalContributionInputException extends RuntimeException {

    public InvalidGoalContributionInputException(String message) {
        super(message);
    }
}
