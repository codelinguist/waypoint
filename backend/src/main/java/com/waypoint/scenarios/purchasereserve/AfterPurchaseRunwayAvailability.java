package com.waypoint.scenarios.purchasereserve;

/**
 * Whether an after-purchase emergency-fund runway could be computed. When a
 * purchase exceeds the available reserve, the post-purchase cash balance is
 * negative and cannot be handed to the runway calculator, which rejects a
 * negative reserve rather than silently clamping it to zero.
 */
public enum AfterPurchaseRunwayAvailability {
    AVAILABLE,
    INSUFFICIENT_CASH
}
