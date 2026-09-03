package com.waypoint.household.web.dto;

import com.waypoint.household.Asset;
import com.waypoint.household.AssetType;
import com.waypoint.household.Liquidity;
import com.waypoint.household.SourceType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AssetResponse(
        UUID id,
        UUID householdId,
        String name,
        AssetType assetType,
        BigDecimal estimatedValue,
        BigDecimal planningValue,
        String currency,
        LocalDate valuedAt,
        Liquidity liquidity,
        SourceType sourceType,
        Instant createdAt,
        Instant updatedAt
) {

    public static AssetResponse from(Asset asset) {
        return new AssetResponse(
                asset.getId(),
                asset.getHousehold().getId(),
                asset.getName(),
                asset.getAssetType(),
                asset.getEstimatedValue(),
                asset.getPlanningValue(),
                asset.getCurrency(),
                asset.getValuedAt(),
                asset.getLiquidity(),
                asset.getSourceType(),
                asset.getCreatedAt(),
                asset.getUpdatedAt()
        );
    }
}
