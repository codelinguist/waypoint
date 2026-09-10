package com.waypoint.planning.multigoalfunding;

/**
 * Thrown when {@link MultiGoalFundingCheckCalculator} is invoked with inputs
 * that violate this calculation's own invariants, independent of any
 * transport (HTTP) validation that may have already run.
 */
public class InvalidMultiGoalFundingInputException extends RuntimeException {

    public InvalidMultiGoalFundingInputException(String message) {
        super(message);
    }
}
