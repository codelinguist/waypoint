package com.waypoint.household;

import java.math.BigDecimal;

/**
 * The signed, later-minus-earlier change in one currency's {@link
 * CurrencyTotals} between two {@link FinancialSnapshot}s. A currency present
 * in only one of the two snapshots is compared against zero rather than
 * omitted, so its full value still surfaces as a delta.
 */
public record CurrencyTotalsDelta(
        String currency,
        BigDecimal assetTotalDelta,
        BigDecimal liabilityTotalDelta,
        BigDecimal netWorthDelta
) {

    static CurrencyTotalsDelta of(String currency, CurrencyTotals earlier, CurrencyTotals later) {
        return new CurrencyTotalsDelta(
                currency,
                later.assetTotal().subtract(earlier.assetTotal()),
                later.liabilityTotal().subtract(earlier.liabilityTotal()),
                later.netWorth().subtract(earlier.netWorth())
        );
    }
}
