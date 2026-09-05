package com.waypoint.planning.futurevalue.web.dto;

import com.waypoint.planning.futurevalue.FutureValueResult;
import java.math.BigDecimal;
import java.util.List;

public record FutureValueResponse(
        String currency,
        BigDecimal startingPrincipal,
        BigDecimal monthlyContribution,
        BigDecimal annualRatePercentage,
        int projectionMonths,
        BigDecimal endingValue,
        BigDecimal totalContributed,
        BigDecimal totalGrowth,
        String conventions,
        List<FutureValueRowResponse> schedule
) {

    public static FutureValueResponse from(FutureValueResult result) {
        return new FutureValueResponse(
                result.currency(),
                result.startingPrincipal(),
                result.monthlyContribution(),
                result.annualRatePercentage(),
                result.projectionMonths(),
                result.endingValue(),
                result.totalContributed(),
                result.totalGrowth(),
                result.conventions(),
                result.schedule().stream().map(FutureValueRowResponse::from).toList()
        );
    }
}
