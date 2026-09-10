package com.waypoint.scenarios.debtprepayment;

/**
 * Thrown when the domain calculator is invoked with values that violate its input invariants,
 * independent of any transport-layer validation.
 */
public class InvalidDebtPrepaymentInputException extends RuntimeException {

    public InvalidDebtPrepaymentInputException(String message) {
        super(message);
    }
}
