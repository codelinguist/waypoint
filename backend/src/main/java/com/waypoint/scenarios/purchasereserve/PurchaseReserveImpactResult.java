package com.waypoint.scenarios.purchasereserve;

import com.waypoint.planning.runway.EmergencyFundRunwayResult;
import java.math.BigDecimal;

/**
 * Deterministic, neutral scenario facts describing how one explicitly
 * supplied cash purchase would change reserve coverage against an
 * explicitly supplied reserve floor. Nothing here is read from or written
 * to canonical household state, and no field expresses an approval, denial,
 * or recommendation of the purchase or the floor.
 *
 * @param currency                        normalized (uppercase) three-letter currency code echoed from the request
 * @param availableReserve                echoed available cash reserve before the purchase
 * @param purchaseAmount                  echoed proposed purchase amount
 * @param monthlyExpenses                 echoed monthly expenses, unchanged by the purchase
 * @param monthlyNetIncome                echoed monthly net income, unchanged by the purchase
 * @param minimumReserve                  echoed reserve floor the caller chose to compare against
 * @param reserveAfterPurchase            {@code availableReserve - purchaseAmount}; may be negative
 * @param purchaseFundingGap              {@code max(0, -reserveAfterPurchase)}: the cash shortfall, if any, to fund the purchase from the supplied reserve alone
 * @param purchaseFitsAvailableCash       {@code true} when {@code purchaseFundingGap} is zero
 * @param baselineReserveFloorGap         {@code max(0, minimumReserve - availableReserve)}: any pre-existing floor gap, before the purchase
 * @param reserveFloorGapAfterPurchase    {@code max(0, minimumReserve - reserveAfterPurchase)}: the floor gap after the purchase
 * @param reserveMeetsFloorAfterPurchase  {@code true} when {@code reserveFloorGapAfterPurchase} is zero
 * @param beforePurchaseRunway            emergency-fund runway computed from {@code availableReserve} with unchanged income and expenses
 * @param afterPurchaseRunwayAvailability whether an after-purchase runway could be computed
 * @param afterPurchaseRunway             emergency-fund runway computed from {@code reserveAfterPurchase}; {@code null} when {@code afterPurchaseRunwayAvailability} is {@code INSUFFICIENT_CASH}
 */
public record PurchaseReserveImpactResult(
        String currency,
        BigDecimal availableReserve,
        BigDecimal purchaseAmount,
        BigDecimal monthlyExpenses,
        BigDecimal monthlyNetIncome,
        BigDecimal minimumReserve,
        BigDecimal reserveAfterPurchase,
        BigDecimal purchaseFundingGap,
        boolean purchaseFitsAvailableCash,
        BigDecimal baselineReserveFloorGap,
        BigDecimal reserveFloorGapAfterPurchase,
        boolean reserveMeetsFloorAfterPurchase,
        EmergencyFundRunwayResult beforePurchaseRunway,
        AfterPurchaseRunwayAvailability afterPurchaseRunwayAvailability,
        EmergencyFundRunwayResult afterPurchaseRunway
) {
}
