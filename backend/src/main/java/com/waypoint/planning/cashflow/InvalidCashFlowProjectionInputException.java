package com.waypoint.planning.cashflow;

/**
 * Thrown when the domain calculator is invoked with values that violate its input invariants,
 * independent of any transport-layer validation.
 */
public class InvalidCashFlowProjectionInputException extends RuntimeException {

    public InvalidCashFlowProjectionInputException(String message) {
        super(message);
    }
}
