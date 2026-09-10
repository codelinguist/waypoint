package com.waypoint.position.web.dto;

import com.waypoint.household.Asset;
import com.waypoint.household.AssetType;
import com.waypoint.household.Liquidity;
import com.waypoint.household.SourceType;
import java.time.LocalDate;
import java.util.UUID;

public record PositionAssetResponse(
        UUID id,
        String name,
        AssetType assetType,
        String estimatedValue,
        String planningValue,
        String currency,
        LocalDate valuedAt,
        Liquidity liquidity,
        SourceType sourceType
) {

    public static PositionAssetResponse from(Asset asset) {
        return new PositionAssetResponse(
                asset.getId(),
                asset.getName(),
                asset.getAssetType(),
                MoneyFormat.plain(asset.getEstimatedValue()),
                MoneyFormat.plain(asset.getPlanningValue()),
                asset.getCurrency(),
                asset.getValuedAt(),
                asset.getLiquidity(),
                asset.getSourceType()
        );
    }
}
