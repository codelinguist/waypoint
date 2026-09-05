package com.waypoint.planning.futurevalue;

/**
 * Thrown when the domain calculator is invoked with values that violate its input invariants,
 * independent of any transport-layer validation.
 */
public class InvalidFutureValueInputException extends RuntimeException {

    public InvalidFutureValueInputException(String message) {
        super(message);
    }
}
