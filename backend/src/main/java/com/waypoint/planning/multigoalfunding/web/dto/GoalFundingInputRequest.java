package com.waypoint.planning.multigoalfunding.web.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record GoalFundingInputRequest(
        @NotBlank(message = "reference must not be blank")
        @Size(max = 64, message = "reference must be at most 64 characters")
        String reference,

        @NotNull(message = "targetAmount must not be null")
        @DecimalMin(value = "0", inclusive = false, message = "targetAmount must be greater than zero")
        @Digits(integer = 17, fraction = 2, message = "targetAmount must have at most 17 integer digits and 2 fraction digits")
        BigDecimal targetAmount,

        @NotNull(message = "currentAmount must not be null")
        @DecimalMin(value = "0", message = "currentAmount must not be negative")
        @Digits(integer = 17, fraction = 2, message = "currentAmount must have at most 17 integer digits and 2 fraction digits")
        BigDecimal currentAmount,

        @NotNull(message = "contributionMonths must not be null")
        @Min(value = 1, message = "contributionMonths must be at least 1")
        @Max(value = 1200, message = "contributionMonths must be at most 1200")
        @JsonDeserialize(using = WholeNumberDeserializer.class)
        Integer contributionMonths
) {
}
