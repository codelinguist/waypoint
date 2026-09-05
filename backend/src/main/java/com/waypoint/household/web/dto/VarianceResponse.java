package com.waypoint.household.web.dto;

import com.waypoint.household.PlanVersusActualVariance;
import com.waypoint.household.VarianceDirection;
import java.math.BigDecimal;

public record VarianceResponse(
        BigDecimal planned,
        BigDecimal actual,
        BigDecimal variance,
        VarianceDirection direction
) {

    public static VarianceResponse from(PlanVersusActualVariance variance) {
        return new VarianceResponse(variance.planned(), variance.actual(), variance.variance(), variance.direction());
    }
}
