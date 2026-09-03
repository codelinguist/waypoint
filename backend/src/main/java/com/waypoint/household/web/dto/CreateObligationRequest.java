package com.waypoint.household.web.dto;

import com.waypoint.household.Frequency;
import com.waypoint.household.ObligationType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateObligationRequest(
        @NotBlank(message = "name must not be blank")
        String name,

        @NotNull(message = "obligationType must not be null")
        ObligationType obligationType,

        @NotNull(message = "amount must not be null")
        @DecimalMin(value = "0", message = "amount must not be negative")
        @Digits(integer = 17, fraction = 2, message = "amount must have at most 17 integer digits and 2 fraction digits")
        BigDecimal amount,

        @NotNull(message = "frequency must not be null")
        Frequency frequency,

        @NotBlank(message = "currency must not be blank")
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "currency must be a 3-letter currency code")
        String currency,

        @NotNull(message = "startDate must not be null")
        LocalDate startDate,

        LocalDate endDate
) {
}
