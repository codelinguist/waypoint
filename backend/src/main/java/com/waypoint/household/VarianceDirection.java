package com.waypoint.household;

/**
 * The mathematical sign of an actual-minus-plan variance. Deliberately
 * neutral: whether {@code ABOVE_PLAN} or {@code BELOW_PLAN} is favorable
 * depends on the measure and household priorities, which this analysis does
 * not decide.
 */
public enum VarianceDirection {
    ABOVE_PLAN,
    BELOW_PLAN,
    ON_PLAN
}
