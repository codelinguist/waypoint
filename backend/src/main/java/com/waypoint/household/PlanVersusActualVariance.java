package com.waypoint.household;

import java.math.BigDecimal;

/**
 * A signed actual-minus-plan variance for one measure, alongside the planned
 * and actual values it was computed from and its neutral {@link
 * VarianceDirection}.
 */
public record PlanVersusActualVariance(
        BigDecimal planned,
        BigDecimal actual,
        BigDecimal variance,
        VarianceDirection direction
) {

    static PlanVersusActualVariance of(BigDecimal planned, BigDecimal actual) {
        BigDecimal variance = actual.subtract(planned);
        VarianceDirection direction = switch (variance.signum()) {
            case 1 -> VarianceDirection.ABOVE_PLAN;
            case -1 -> VarianceDirection.BELOW_PLAN;
            default -> VarianceDirection.ON_PLAN;
        };
        return new PlanVersusActualVariance(planned, actual, variance, direction);
    }
}
