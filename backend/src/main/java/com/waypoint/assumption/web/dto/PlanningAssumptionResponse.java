package com.waypoint.assumption.web.dto;

import com.waypoint.assumption.PlanningAssumption;
import com.waypoint.household.SourceType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PlanningAssumptionResponse(
        UUID id,
        UUID householdId,
        String name,
        String value,
        String valueType,
        String notes,
        LocalDate effectiveFrom,
        LocalDate effectiveUntil,
        LocalDate reviewDate,
        SourceType sourceType,
        UUID supersededBy,
        Instant createdAt
) {

    public static PlanningAssumptionResponse from(PlanningAssumption assumption) {
        return new PlanningAssumptionResponse(
                assumption.getId(),
                assumption.getHousehold().getId(),
                assumption.getName(),
                assumption.getValue(),
                assumption.getValueType(),
                assumption.getNotes(),
                assumption.getEffectiveFrom(),
                assumption.getEffectiveUntil(),
                assumption.getReviewDate(),
                assumption.getSourceType(),
                assumption.getSupersededById(),
                assumption.getCreatedAt()
        );
    }
}
