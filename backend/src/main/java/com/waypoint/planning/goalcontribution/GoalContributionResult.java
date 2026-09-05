package com.waypoint.planning.goalcontribution;

import java.math.BigDecimal;

/**
 * Deterministic result of an equal-monthly-contribution calculation over
 * explicit, caller-supplied inputs. Nothing in this result is read from or
 * written to canonical household state.
 *
 * @param currency               normalized (uppercase) three-letter currency code echoed from the request
 * @param targetAmount           echoed goal target amount
 * @param currentAmount          echoed current amount already saved toward the goal
 * @param contributionMonths     echoed number of equal monthly contributions
 * @param remainingAmount        {@code max(targetAmount - currentAmount, 0)}
 * @param monthlyContribution    equal monthly amount, rounded up to 2 decimal places so total contributions never fall short
 * @param totalContributions     {@code monthlyContribution * contributionMonths}
 * @param projectedAmount        {@code currentAmount + totalContributions}
 * @param amountAboveTarget      {@code max(projectedAmount - targetAmount, 0)}
 * @param status                 whether the goal is already funded or still requires contributions
 */
public record GoalContributionResult(
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
}
