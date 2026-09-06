package com.waypoint.planning.cashflow;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/**
 * Deterministic result of a constant monthly cash-flow projection over explicit,
 * caller-supplied inputs. Nothing in this result is read from or written to canonical
 * household state; it is a temporary modeling assumption, not a forecast, recommendation,
 * or approved decision.
 *
 * @param currency                    normalized (uppercase) three-letter currency code echoed from the request
 * @param startMonth                  echoed first projected month
 * @param startingCash                echoed non-negative starting cash balance
 * @param monthlyInflow               echoed constant monthly inflow
 * @param monthlyOutflow              echoed constant monthly outflow
 * @param months                      echoed number of projected months
 * @param rows                        ordered monthly rows, one per projected month
 * @param endingCash                  closing cash of the final projected month
 * @param lowestClosingBalance        the lowest closing balance across every projected month
 * @param lowestClosingBalanceMonth   the first month at which {@code lowestClosingBalance} occurs
 * @param firstNegativeMonth          the first month whose closing balance is strictly below zero, or
 *                                    {@code null} if none is
 * @param status                      whether every closing balance remained non-negative or became negative
 */
public record CashFlowProjectionResult(
        String currency,
        YearMonth startMonth,
        BigDecimal startingCash,
        BigDecimal monthlyInflow,
        BigDecimal monthlyOutflow,
        int months,
        List<CashFlowProjectionRow> rows,
        BigDecimal endingCash,
        BigDecimal lowestClosingBalance,
        YearMonth lowestClosingBalanceMonth,
        YearMonth firstNegativeMonth,
        CashFlowProjectionStatus status
) {
}
