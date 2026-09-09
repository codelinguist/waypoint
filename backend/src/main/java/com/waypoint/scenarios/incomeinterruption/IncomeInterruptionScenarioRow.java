package com.waypoint.scenarios.incomeinterruption;

import java.math.BigDecimal;

/**
 * One month of either the baseline (uninterrupted) or scenario (interrupted) path.
 *
 * <p>Reconciles as {@code openingCash + income - expenses = closingCash}, and this row's
 * {@code closingCash} equals the next row's {@code openingCash} within the same path.
 */
public record IncomeInterruptionScenarioRow(
        int month,
        BigDecimal openingCash,
        BigDecimal income,
        BigDecimal expenses,
        BigDecimal netCashFlow,
        BigDecimal closingCash
) {
}
