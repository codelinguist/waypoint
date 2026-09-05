package com.waypoint.planning.runway;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * The deterministic result of one constant-input emergency-fund runway
 * calculation. Every field is derived from the caller-supplied inputs on
 * this record alone; nothing here is read from or written to household
 * state.
 */
public record EmergencyFundRunwayResult(
        String currency,
        BigDecimal availableReserve,
        BigDecimal monthlyExpenses,
        BigDecimal monthlyNetIncome,
        BigDecimal monthlyShortfall,
        RunwayStatus status,
        BigDecimal runwayMonths,
        BigInteger fullMonthsCovered
) {
}
