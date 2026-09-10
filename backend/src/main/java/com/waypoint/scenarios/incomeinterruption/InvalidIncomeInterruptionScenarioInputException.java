package com.waypoint.scenarios.incomeinterruption;

/**
 * Thrown when {@link IncomeInterruptionScenarioCalculator} is invoked with inputs that violate
 * this calculation's own invariants, independent of any transport (HTTP) validation that may
 * have already run.
 */
public class InvalidIncomeInterruptionScenarioInputException extends RuntimeException {

    public InvalidIncomeInterruptionScenarioInputException(String message) {
        super(message);
    }
}
