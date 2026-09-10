package com.waypoint.scenarios.debtprepayment;

import com.waypoint.planning.debtamortization.DebtAmortizationResult;
import java.math.BigDecimal;

/**
 * Result of comparing an explicit immediate principal prepayment against continuing the same
 * fixed monthly payment on the un-prepaid balance.
 *
 * <p>{@code lifetimeInterestSaved}, {@code payoffMonthsSaved}, and {@code lifetimeCashSaved} are
 * only populated when both {@code baseline} and {@code scenario} reach {@code PAID_OFF}; otherwise
 * they are {@code null} and {@code comparisonUnavailableReason} explains why a truncated or
 * non-amortizing total cannot be compared against a lifetime total.
 */
public record DebtPrepaymentComparisonResult(
        BigDecimal principal,
        BigDecimal monthlyInterestRate,
        BigDecimal monthlyPayment,
        String currency,
        BigDecimal immediatePrepayment,
        DebtAmortizationResult baseline,
        DebtAmortizationResult scenario,
        BigDecimal scenarioTotalCashPaid,
        BigDecimal lifetimeInterestSaved,
        Integer payoffMonthsSaved,
        BigDecimal lifetimeCashSaved,
        String comparisonUnavailableReason
) {
}
