package com.waypoint.planning.goalcontribution.web.dto;

import com.waypoint.planning.goalcontribution.GoalContributionResult;
import com.waypoint.planning.goalcontribution.GoalContributionStatus;
import java.math.BigDecimal;

public record GoalContributionResponse(
        String currency,
        BigDecimal targetAmount,
        BigDecimal currentAmount,
        int contributionMonths,
        BigDecimal remainingAmount,
        BigDecimal monthlyContribution,
        BigDecimal totalContributions,
        BigDecimal projectedAmount,
        BigDecimal amountAboveTarget,
        GoalContributionStatus status
) {
    public static GoalContributionResponse from(GoalContributionResult result) {
        return new GoalContributionResponse(
                result.currency(),
                result.targetAmount(),
                result.currentAmount(),
                result.contributionMonths(),
                result.remainingAmount(),
                result.monthlyContribution(),
                result.totalContributions(),
                result.projectedAmount(),
                result.amountAboveTarget(),
                result.status()
        );
    }
}
