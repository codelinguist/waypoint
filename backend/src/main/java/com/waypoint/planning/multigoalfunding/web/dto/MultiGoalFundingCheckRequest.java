package com.waypoint.planning.multigoalfunding.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record MultiGoalFundingCheckRequest(
        @NotBlank(message = "currency must not be blank")
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "currency must be a 3-letter currency code")
        String currency,

        @NotNull(message = "availableMonthlyBudget must not be null")
        @DecimalMin(value = "0", message = "availableMonthlyBudget must not be negative")
        @Digits(integer = 17, fraction = 2, message = "availableMonthlyBudget must have at most 17 integer digits and 2 fraction digits")
        BigDecimal availableMonthlyBudget,

        @NotEmpty(message = "goals must not be empty")
        @Size(max = 50, message = "goals must contain at most 50 entries")
        List<@NotNull(message = "goals must not contain null entries") @Valid GoalFundingInputRequest> goals
) {
}
