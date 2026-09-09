package com.waypoint.scenarios.incomeinterruption;

import java.math.BigDecimal;
import java.util.List;

/**
 * Deterministic result of comparing a baseline (uninterrupted) income path against a scenario
 * path with a caller-defined temporary income interruption, both starting from the same opening
 * reserve and expenses. Nothing in this result is read from or written to canonical household
 * state; it is a temporary modeling assumption, not a forecast, recommendation, or approved
 * decision. Negative balances are preserved as modeled funding gaps, not automatic borrowing.
 *
 * @param currency                          normalized (uppercase) three-letter currency code echoed from the request
 * @param openingReserve                    echoed starting cash balance, shared by both paths
 * @param normalMonthlyNetIncome            echoed monthly net income outside the interruption interval
 * @param interruptedMonthlyNetIncome       echoed monthly net income during the interruption interval
 * @param monthlyExpenses                   echoed constant monthly expenses, shared by both paths
 * @param horizonMonths                     echoed number of projected months
 * @param interruptionStartMonth            echoed first interrupted month (1-based, inclusive)
 * @param interruptionMonths                echoed length of the interruption in months
 * @param baselineRows                      ordered monthly rows at {@code normalMonthlyNetIncome} throughout
 * @param scenarioRows                      ordered monthly rows with {@code interruptedMonthlyNetIncome} applied
 *                                          for the inclusive interval starting at {@code interruptionStartMonth}
 *                                          and lasting {@code interruptionMonths}, normal income before and after
 * @param closingDeltas                     one entry per month: {@code scenarioRow.closingCash - baselineRow.closingCash}
 * @param endingCash                        scenario closing cash of the final projected month
 * @param minimumCash                       the lowest value across {@code openingReserve} and every scenario closing
 *                                          cash, i.e. the minimum cash the household ever holds under the scenario
 * @param firstNegativeMonth                the first month whose scenario closing balance is strictly below zero,
 *                                          or {@code null} if none is
 * @param additionalOpeningReserveNeeded    {@code max(0, -minimumCash)}: the extra opening reserve, on top of
 *                                          {@code openingReserve}, that would have kept the scenario path's
 *                                          minimum cash at or above zero
 */
public record IncomeInterruptionScenarioResult(
        String currency,
        BigDecimal openingReserve,
        BigDecimal normalMonthlyNetIncome,
        BigDecimal interruptedMonthlyNetIncome,
        BigDecimal monthlyExpenses,
        int horizonMonths,
        int interruptionStartMonth,
        int interruptionMonths,
        List<IncomeInterruptionScenarioRow> baselineRows,
        List<IncomeInterruptionScenarioRow> scenarioRows,
        List<BigDecimal> closingDeltas,
        BigDecimal endingCash,
        BigDecimal minimumCash,
        Integer firstNegativeMonth,
        BigDecimal additionalOpeningReserveNeeded
) {
}
