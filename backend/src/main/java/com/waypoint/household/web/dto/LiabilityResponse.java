package com.waypoint.household.web.dto;

import com.waypoint.household.Liability;
import com.waypoint.household.LiabilityType;
import com.waypoint.household.SourceType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record LiabilityResponse(
        UUID id,
        UUID householdId,
        String name,
        LiabilityType liabilityType,
        BigDecimal outstandingBalance,
        String currency,
        LocalDate balanceAsOf,
        SourceType sourceType,
        Instant createdAt,
        Instant updatedAt
) {

    public static LiabilityResponse from(Liability liability) {
        return new LiabilityResponse(
                liability.getId(),
                liability.getHousehold().getId(),
                liability.getName(),
                liability.getLiabilityType(),
                liability.getOutstandingBalance(),
                liability.getCurrency(),
                liability.getBalanceAsOf(),
                liability.getSourceType(),
                liability.getCreatedAt(),
                liability.getUpdatedAt()
        );
    }
}
