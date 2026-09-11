package com.waypoint.household.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateAssetValuationRequest(
        @NotNull(message = "estimatedValue must not be null")
        @DecimalMin(value = "0", message = "estimatedValue must not be negative")
        @Digits(integer = 17, fraction = 2, message = "estimatedValue must have at most 17 integer digits and 2 fraction digits")
        BigDecimal estimatedValue,

        @NotNull(message = "planningValue must not be null")
        @DecimalMin(value = "0", message = "planningValue must not be negative")
        @Digits(integer = 17, fraction = 2, message = "planningValue must have at most 17 integer digits and 2 fraction digits")
        BigDecimal planningValue,

        @NotNull(message = "valuedAt must not be null")
        @PastOrPresent(message = "valuedAt must not be in the future")
        LocalDate valuedAt,

        @NotBlank(message = "reason must not be blank")
        @Size(max = 500, message = "reason must be at most 500 characters")
        String reason,

        @NotNull(message = "expectedRevision must not be null")
        @PositiveOrZero(message = "expectedRevision must not be negative")
        Long expectedRevision
) {
}
