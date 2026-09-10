package com.waypoint.scenarios.purchasereserve;

/**
 * Thrown when {@link PurchaseReserveImpactCalculator} is invoked with inputs
 * that violate this calculation's own invariants, independent of any
 * transport (HTTP) validation that may have already run.
 */
public class InvalidPurchaseReserveImpactInputException extends RuntimeException {

    public InvalidPurchaseReserveImpactInputException(String message) {
        super(message);
    }
}
