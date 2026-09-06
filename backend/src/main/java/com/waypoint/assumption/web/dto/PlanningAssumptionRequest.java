package com.waypoint.assumption.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Shared request shape for both creating a first version and superseding an
 * existing one — a supersession is itself the creation of a new version, so
 * the same fields apply.
 */
public record PlanningAssumptionRequest(
        @NotBlank(message = "name must not be blank")
        @Size(max = 255, message = "name must be at most 255 characters")
        String name,

        @NotBlank(message = "value must not be blank")
        @Size(max = 2000, message = "value must be at most 2000 characters")
        String value,

        @NotBlank(message = "valueType must not be blank")
        @Size(max = 100, message = "valueType must be at most 100 characters")
        String valueType,

        @Size(max = 2000, message = "notes must be at most 2000 characters")
        String notes,

        @NotNull(message = "effectiveFrom must not be null")
        LocalDate effectiveFrom,

        LocalDate effectiveUntil,

        @NotNull(message = "reviewDate must not be null")
        LocalDate reviewDate
) {
}
