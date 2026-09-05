package com.waypoint.assumption.web.dto;

import com.waypoint.assumption.PlanningAssumption;
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
        String sourceType,
        Instant createdAt,
        boolean superseded,
        UUID supersededById
) {

    public static PlanningAssumptionResponse from(PlanningAssumption assumption) {
        PlanningAssumption supersededBy = assumption.getSupersededBy();
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
                assumption.getSourceType().name(),
                assumption.getCreatedAt(),
                supersededBy != null,
                supersededBy != null ? supersededBy.getId() : null
        );
    }
}
