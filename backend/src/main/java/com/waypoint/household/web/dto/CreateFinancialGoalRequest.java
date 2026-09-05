package com.waypoint.household.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateFinancialGoalRequest(
        @NotBlank(message = "name must not be blank")
        String name,

        @NotNull(message = "targetAmount must not be null")
        @DecimalMin(value = "0", inclusive = false, message = "targetAmount must be greater than 0")
        @Digits(integer = 17, fraction = 2, message = "targetAmount must have at most 17 integer digits and 2 fraction digits")
        BigDecimal targetAmount,

        @NotBlank(message = "currency must not be blank")
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "currency must be a 3-letter currency code")
        String currency,

        @NotNull(message = "targetDate must not be null")
        @FutureOrPresent(message = "targetDate must not be in the past")
        LocalDate targetDate,

        @NotNull(message = "priority must not be null")
        @Min(value = 1, message = "priority must be a positive integer")
        Integer priority,

        @NotNull(message = "currentAmount must not be null")
        @DecimalMin(value = "0", message = "currentAmount must not be negative")
        @Digits(integer = 17, fraction = 2, message = "currentAmount must have at most 17 integer digits and 2 fraction digits")
        BigDecimal currentAmount
) {
}
