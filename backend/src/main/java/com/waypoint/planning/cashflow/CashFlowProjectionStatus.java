package com.waypoint.planning.cashflow;

/**
 * Outcome of a constant monthly cash-flow projection.
 *
 * <p>This model requires a non-negative starting cash balance, so a
 * {@code STARTS_NEGATIVE} status is intentionally not modeled here.
 */
public enum CashFlowProjectionStatus {

    /** Every projected closing balance stayed at or above zero. */
    REMAINS_NONNEGATIVE,

    /** At least one projected closing balance fell strictly below zero. */
    BECOMES_NEGATIVE
}
