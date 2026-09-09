package com.waypoint.scenarios.purchasereserve.web.dto;

import com.waypoint.planning.runway.web.dto.EmergencyFundRunwayResponse;
import com.waypoint.scenarios.purchasereserve.AfterPurchaseRunwayAvailability;
import com.waypoint.scenarios.purchasereserve.PurchaseReserveImpactResult;
import java.math.BigDecimal;

public record PurchaseReserveImpactResponse(
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
        EmergencyFundRunwayResponse beforePurchaseRunway,
        AfterPurchaseRunwayAvailability afterPurchaseRunwayAvailability,
        EmergencyFundRunwayResponse afterPurchaseRunway,
        String modelNote
) {

    private static final String MODEL_NOTE =
            "Neutral, read-only scenario facts computed only from the supplied inputs; this result does not "
            + "approve, deny, or recommend a purchase or reserve floor, and it does not persist or represent "
            + "any household decision. Before/after coverage reuses the emergency-fund runway calculator's "
            + "constant-input convention, which excludes any change in income, spending, interest, inflation, "
            + "or timing within a month.";

    public static PurchaseReserveImpactResponse from(PurchaseReserveImpactResult result) {
        EmergencyFundRunwayResponse afterPurchaseRunway = result.afterPurchaseRunway() == null
                ? null
                : EmergencyFundRunwayResponse.from(result.afterPurchaseRunway());
        return new PurchaseReserveImpactResponse(
                result.currency(),
                result.availableReserve(),
                result.purchaseAmount(),
                result.monthlyExpenses(),
                result.monthlyNetIncome(),
                result.minimumReserve(),
                result.reserveAfterPurchase(),
                result.purchaseFundingGap(),
                result.purchaseFitsAvailableCash(),
                result.baselineReserveFloorGap(),
                result.reserveFloorGapAfterPurchase(),
                result.reserveMeetsFloorAfterPurchase(),
                EmergencyFundRunwayResponse.from(result.beforePurchaseRunway()),
                result.afterPurchaseRunwayAvailability(),
                afterPurchaseRunway,
                MODEL_NOTE
        );
    }
}
