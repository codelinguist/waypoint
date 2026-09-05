package com.waypoint.planning.debtamortization;

import java.math.BigDecimal;

/**
 * One month of a fixed-payment amortization schedule.
 *
 * <p>Reconciles as {@code openingBalance + interest - payment = closingBalance}.
 */
public record DebtAmortizationRow(
        int month,
        BigDecimal openingBalance,
        BigDecimal interest,
        BigDecimal payment,
        BigDecimal principalRepaid,
        BigDecimal closingBalance
) {
}
