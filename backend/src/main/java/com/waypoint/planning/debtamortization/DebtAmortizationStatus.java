package com.waypoint.planning.debtamortization;

/**
 * Outcome of a fixed-payment amortization calculation.
 */
public enum DebtAmortizationStatus {

    /** The balance reached zero at or before the computation horizon. */
    PAID_OFF,

    /** The fixed payment does not exceed the first month's interest, so the balance never decreases. */
    NON_AMORTIZING,

    /** The balance is still decreasing but has not reached zero after the computation horizon. */
    HORIZON_LIMIT
}
