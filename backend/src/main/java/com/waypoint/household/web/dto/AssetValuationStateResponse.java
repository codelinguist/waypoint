package com.waypoint.household.web.dto;

import com.waypoint.household.Asset;
import com.waypoint.household.SourceType;
import java.time.LocalDate;
import java.util.UUID;

/**
 * The asset's current recorded valuation plus its opaque revision, for
 * conditional submission. Reflects the current state even when no update
 * has ever been made (the state as of creation, revision {@code 0}).
 */
public record AssetValuationStateResponse(
        UUID assetId,
        UUID householdId,
        String estimatedValue,
        String planningValue,
        String currency,
        LocalDate valuedAt,
        SourceType sourceType,
        long revision
) {

    public static AssetValuationStateResponse from(Asset asset) {
        return new AssetValuationStateResponse(
                asset.getId(),
                asset.getHousehold().getId(),
                MoneyFormat.plain(asset.getEstimatedValue()),
                MoneyFormat.plain(asset.getPlanningValue()),
                asset.getCurrency(),
                asset.getValuedAt(),
                asset.getSourceType(),
                asset.getRevision()
        );
    }
}
