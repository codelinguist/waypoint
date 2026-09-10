package com.waypoint.planning.multigoalfunding.web.dto;

import com.waypoint.planning.multigoalfunding.MultiGoalFundingCheckResult;
import com.waypoint.planning.multigoalfunding.MultiGoalFundingStatus;
import java.math.BigDecimal;
import java.util.List;

public record MultiGoalFundingCheckResponse(
        String currency,
        BigDecimal availableMonthlyBudget,
        List<GoalFundingResultResponse> goalResults,
        BigDecimal totalRequiredMonthlyContribution,
        BigDecimal budgetMinusRequired,
        BigDecimal shortfall,
        BigDecimal unallocatedBudget,
        MultiGoalFundingStatus status
) {
    public static MultiGoalFundingCheckResponse from(MultiGoalFundingCheckResult result) {
        return new MultiGoalFundingCheckResponse(
                result.currency(),
                result.availableMonthlyBudget(),
                result.goalResults().stream().map(GoalFundingResultResponse::from).toList(),
                result.totalRequiredMonthlyContribution(),
                result.budgetMinusRequired(),
                result.shortfall(),
                result.unallocatedBudget(),
                result.status()
        );
    }
}
