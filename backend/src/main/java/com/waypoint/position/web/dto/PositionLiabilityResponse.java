package com.waypoint.position.web.dto;

import com.waypoint.household.Liability;
import com.waypoint.household.LiabilityType;
import com.waypoint.household.SourceType;
import java.time.LocalDate;
import java.util.UUID;

public record PositionLiabilityResponse(
        UUID id,
        String name,
        LiabilityType liabilityType,
        String outstandingBalance,
        String currency,
        LocalDate balanceAsOf,
        SourceType sourceType
) {

    public static PositionLiabilityResponse from(Liability liability) {
        return new PositionLiabilityResponse(
                liability.getId(),
                liability.getName(),
                liability.getLiabilityType(),
                MoneyFormat.plain(liability.getOutstandingBalance()),
                liability.getCurrency(),
                liability.getBalanceAsOf(),
                liability.getSourceType()
        );
    }
}
