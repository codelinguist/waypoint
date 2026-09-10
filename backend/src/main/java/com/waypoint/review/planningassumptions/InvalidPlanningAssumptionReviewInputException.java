package com.waypoint.review.planningassumptions;

/**
 * Thrown when {@link PlanningAssumptionReviewCalculator} is invoked with
 * inputs that violate this review's own invariants, independent of any
 * transport (HTTP) validation that may have already run.
 */
public class InvalidPlanningAssumptionReviewInputException extends RuntimeException {

    public InvalidPlanningAssumptionReviewInputException(String message) {
        super(message);
    }
}
