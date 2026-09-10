package com.waypoint.assumption.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Carries the replacement's full field set, including {@code name}, so the
 * service can confirm the replacement belongs to the same logical
 * assumption before linking it to the version it supersedes.
 */
public record SupersedePlanningAssumptionRequest(
        @NotBlank(message = "name must not be blank")
        @Size(max = 255, message = "name must be at most 255 characters")
        String name,

        @NotBlank(message = "value must not be blank")
        @Size(max = 1000, message = "value must be at most 1000 characters")
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
