package com.waypoint.planning.multigoalfunding;

import java.math.BigDecimal;
import java.util.List;

/**
 * Deterministic result of checking whether several independently modeled
 * goals fit one shared monthly budget, over explicit, caller-supplied
 * inputs. Nothing in this result is read from or written to canonical
 * household state; it is the initial simultaneous monthly funding
 * requirement under zero growth, not an optimizer or an approved allocation.
 *
 * @param currency                       normalized (uppercase) three-letter currency code echoed from the request
 * @param availableMonthlyBudget         echoed available monthly budget
 * @param goalResults                    per-goal results, in caller order
 * @param totalRequiredMonthlyContribution sum of every goal's rounded monthly contribution
 * @param budgetMinusRequired            {@code availableMonthlyBudget - totalRequiredMonthlyContribution}, may be negative
 * @param shortfall                      {@code max(0, -budgetMinusRequired)}
 * @param unallocatedBudget              {@code max(0, budgetMinusRequired)}
 * @param status                         whether the budget covers the total requirement
 */
public record MultiGoalFundingCheckResult(
        String currency,
        BigDecimal availableMonthlyBudget,
        List<GoalFundingCheckItemResult> goalResults,
        BigDecimal totalRequiredMonthlyContribution,
        BigDecimal budgetMinusRequired,
        BigDecimal shortfall,
        BigDecimal unallocatedBudget,
        MultiGoalFundingStatus status
) {
}
