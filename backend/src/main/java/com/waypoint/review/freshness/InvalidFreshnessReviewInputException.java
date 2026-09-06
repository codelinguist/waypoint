package com.waypoint.review.freshness;

/**
 * Thrown when {@link FinancialDataFreshnessCalculator} is invoked with
 * inputs that violate this review's own invariants, independent of any
 * transport (HTTP) validation that may have already run.
 */
public class InvalidFreshnessReviewInputException extends RuntimeException {

    public InvalidFreshnessReviewInputException(String message) {
        super(message);
    }
}
