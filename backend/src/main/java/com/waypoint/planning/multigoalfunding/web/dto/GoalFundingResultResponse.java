package com.waypoint.planning.multigoalfunding.web.dto;

import com.waypoint.planning.goalcontribution.GoalContributionResult;
import com.waypoint.planning.goalcontribution.GoalContributionStatus;
import com.waypoint.planning.multigoalfunding.GoalFundingCheckItemResult;
import java.math.BigDecimal;

public record GoalFundingResultResponse(
        String reference,
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
    public static GoalFundingResultResponse from(GoalFundingCheckItemResult item) {
        GoalContributionResult contribution = item.contribution();
        return new GoalFundingResultResponse(
                item.reference(),
                contribution.targetAmount(),
                contribution.currentAmount(),
                contribution.contributionMonths(),
                contribution.remainingAmount(),
                contribution.monthlyContribution(),
                contribution.totalContributions(),
                contribution.projectedAmount(),
                contribution.amountAboveTarget(),
                contribution.status()
        );
    }
}
