package com.waypoint.planning.debtamortization;

/**
 * Thrown when the domain calculator is invoked with values that violate its input invariants,
 * independent of any transport-layer validation.
 */
public class InvalidDebtAmortizationInputException extends RuntimeException {

    public InvalidDebtAmortizationInputException(String message) {
        super(message);
    }
}
