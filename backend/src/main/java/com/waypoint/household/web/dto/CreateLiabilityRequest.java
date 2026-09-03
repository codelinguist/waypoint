package com.waypoint.household.web.dto;

import com.waypoint.household.LiabilityType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateLiabilityRequest(
        @NotBlank(message = "name must not be blank")
        String name,

        @NotNull(message = "liabilityType must not be null")
        LiabilityType liabilityType,

        @NotNull(message = "outstandingBalance must not be null")
        @DecimalMin(value = "0", message = "outstandingBalance must not be negative")
        @Digits(integer = 17, fraction = 2, message = "outstandingBalance must have at most 17 integer digits and 2 fraction digits")
        BigDecimal outstandingBalance,

        @NotBlank(message = "currency must not be blank")
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "currency must be a 3-letter currency code")
        String currency,

        @NotNull(message = "balanceAsOf must not be null")
        @PastOrPresent(message = "balanceAsOf must not be in the future")
        LocalDate balanceAsOf
) {
}
