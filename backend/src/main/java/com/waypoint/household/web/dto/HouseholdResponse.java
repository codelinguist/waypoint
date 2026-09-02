package com.waypoint.household.web.dto;

import com.waypoint.household.Household;
import java.time.Instant;
import java.util.UUID;

public record HouseholdResponse(
        UUID id,
        String name,
        String baseCurrency,
        Instant createdAt,
        Instant updatedAt
) {

    public static HouseholdResponse from(Household household) {
        return new HouseholdResponse(
                household.getId(),
                household.getName(),
                household.getBaseCurrency(),
                household.getCreatedAt(),
                household.getUpdatedAt()
        );
    }
}
