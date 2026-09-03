package com.waypoint.household.web.dto;

import com.waypoint.household.Frequency;
import com.waypoint.household.Obligation;
import com.waypoint.household.ObligationType;
import com.waypoint.household.SourceType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ObligationResponse(
        UUID id,
        UUID householdId,
        String name,
        ObligationType obligationType,
        BigDecimal amount,
        Frequency frequency,
        String currency,
        LocalDate startDate,
        LocalDate endDate,
        SourceType sourceType,
        Instant createdAt,
        Instant updatedAt
) {

    public static ObligationResponse from(Obligation obligation) {
        return new ObligationResponse(
                obligation.getId(),
                obligation.getHousehold().getId(),
                obligation.getName(),
                obligation.getObligationType(),
                obligation.getAmount(),
                obligation.getFrequency(),
                obligation.getCurrency(),
                obligation.getStartDate(),
                obligation.getEndDate(),
                obligation.getSourceType(),
                obligation.getCreatedAt(),
                obligation.getUpdatedAt()
        );
    }
}
