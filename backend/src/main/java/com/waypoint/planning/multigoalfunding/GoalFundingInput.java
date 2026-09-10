package com.waypoint.planning.multigoalfunding;

import java.math.BigDecimal;

/**
 * One caller-supplied goal to check against a shared monthly budget. Mirrors
 * {@link com.waypoint.planning.goalcontribution.GoalContributionCalculator}'s
 * per-goal inputs, plus a caller-chosen {@code reference} used to correlate
 * each result back to its input in caller order.
 *
 * @param reference          caller-supplied identifier for this goal, unique within the request
 * @param targetAmount       goal target amount
 * @param currentAmount      amount already saved toward the goal
 * @param contributionMonths number of equal monthly contributions this goal starts in the first modeled month
 */
public record GoalFundingInput(
        String reference,
        BigDecimal targetAmount,
        BigDecimal currentAmount,
        int contributionMonths
) {
}
