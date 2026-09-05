package com.waypoint.planning.debtamortization;

import java.math.BigDecimal;
import java.util.List;

/**
 * Result of a fixed-payment amortization calculation, echoing the inputs alongside the
 * computed schedule and totals.
 */
public record DebtAmortizationResult(
        BigDecimal principal,
        BigDecimal monthlyInterestRate,
        BigDecimal monthlyPayment,
        String currency,
        DebtAmortizationStatus status,
        Integer payoffMonths,
        BigDecimal totalPaid,
        BigDecimal totalInterest,
        BigDecimal remainingBalance,
        List<DebtAmortizationRow> schedule
) {
}
