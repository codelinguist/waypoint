package com.waypoint.household.web.dto;

import com.waypoint.household.AssetType;
import com.waypoint.household.SnapshotAssetLineItem;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record SnapshotAssetLineItemResponse(
        UUID id,
        UUID sourceAssetId,
        String name,
        AssetType assetType,
        String currency,
        LocalDate sourceDate,
        BigDecimal value
) {

    public static SnapshotAssetLineItemResponse from(SnapshotAssetLineItem lineItem) {
        return new SnapshotAssetLineItemResponse(
                lineItem.getId(),
                lineItem.getSourceAssetId(),
                lineItem.getName(),
                lineItem.getAssetType(),
                lineItem.getCurrency(),
                lineItem.getSourceDate(),
                lineItem.getValue()
        );
    }
}
