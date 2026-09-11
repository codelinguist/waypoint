package com.waypoint.household.web.dto;

import com.waypoint.household.AssetValuation;
import com.waypoint.household.SourceType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AssetValuationHistoryResponse(
        UUID id,
        UUID assetId,
        UUID householdId,
        long revision,
        String previousEstimatedValue,
        String previousPlanningValue,
        LocalDate previousValuedAt,
        SourceType previousSourceType,
        String newEstimatedValue,
        String newPlanningValue,
        LocalDate newValuedAt,
        SourceType newSourceType,
        String reason,
        Instant recordedAt
) {

    public static AssetValuationHistoryResponse from(AssetValuation valuation) {
        return new AssetValuationHistoryResponse(
                valuation.getId(),
                valuation.getAsset().getId(),
                valuation.getHousehold().getId(),
                valuation.getRevision(),
                MoneyFormat.plain(valuation.getPreviousEstimatedValue()),
                MoneyFormat.plain(valuation.getPreviousPlanningValue()),
                valuation.getPreviousValuedAt(),
                valuation.getPreviousSourceType(),
                MoneyFormat.plain(valuation.getNewEstimatedValue()),
                MoneyFormat.plain(valuation.getNewPlanningValue()),
                valuation.getNewValuedAt(),
                valuation.getNewSourceType(),
                valuation.getReason(),
                valuation.getRecordedAt()
        );
    }
}
