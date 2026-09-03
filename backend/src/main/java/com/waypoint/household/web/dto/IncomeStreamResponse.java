package com.waypoint.household.web.dto;

import com.waypoint.household.CompensationClassification;
import com.waypoint.household.Frequency;
import com.waypoint.household.IncomeCertainty;
import com.waypoint.household.IncomeStream;
import com.waypoint.household.IncomeType;
import com.waypoint.household.SourceType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record IncomeStreamResponse(
        UUID id,
        UUID householdId,
        String name,
        IncomeType incomeType,
        BigDecimal amount,
        Frequency frequency,
        String currency,
        CompensationClassification compensationClassification,
        IncomeCertainty certainty,
        LocalDate startDate,
        LocalDate endDate,
        SourceType sourceType,
        Instant createdAt,
        Instant updatedAt
) {

    public static IncomeStreamResponse from(IncomeStream incomeStream) {
        return new IncomeStreamResponse(
                incomeStream.getId(),
                incomeStream.getHousehold().getId(),
                incomeStream.getName(),
                incomeStream.getIncomeType(),
                incomeStream.getAmount(),
                incomeStream.getFrequency(),
                incomeStream.getCurrency(),
                incomeStream.getCompensationClassification(),
                incomeStream.getCertainty(),
                incomeStream.getStartDate(),
                incomeStream.getEndDate(),
                incomeStream.getSourceType(),
                incomeStream.getCreatedAt(),
                incomeStream.getUpdatedAt()
        );
    }
}
