package com.waypoint.household.web.dto;

import com.waypoint.household.LiabilityType;
import com.waypoint.household.SnapshotLiabilityLineItem;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record SnapshotLiabilityLineItemResponse(
        UUID id,
        UUID sourceLiabilityId,
        String name,
        LiabilityType liabilityType,
        String currency,
        LocalDate sourceDate,
        BigDecimal value
) {

    public static SnapshotLiabilityLineItemResponse from(SnapshotLiabilityLineItem lineItem) {
        return new SnapshotLiabilityLineItemResponse(
                lineItem.getId(),
                lineItem.getSourceLiabilityId(),
                lineItem.getName(),
                lineItem.getLiabilityType(),
                lineItem.getCurrency(),
                lineItem.getSourceDate(),
                lineItem.getValue()
        );
    }
}
