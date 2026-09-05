package com.waypoint.planning.runway.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public record EmergencyFundRunwayRequest(
        @NotNull(message = "availableReserve must not be null")
        @DecimalMin(value = "0", message = "availableReserve must not be negative")
        @Digits(integer = 17, fraction = 2,
                message = "availableReserve must have at most 17 integer digits and 2 fraction digits")
        BigDecimal availableReserve,

        @NotNull(message = "monthlyExpenses must not be null")
        @DecimalMin(value = "0", message = "monthlyExpenses must not be negative")
        @Digits(integer = 17, fraction = 2,
                message = "monthlyExpenses must have at most 17 integer digits and 2 fraction digits")
        BigDecimal monthlyExpenses,

        @NotNull(message = "monthlyNetIncome must not be null")
        @DecimalMin(value = "0", message = "monthlyNetIncome must not be negative")
        @Digits(integer = 17, fraction = 2,
                message = "monthlyNetIncome must have at most 17 integer digits and 2 fraction digits")
        BigDecimal monthlyNetIncome,

        @NotBlank(message = "currency must not be blank")
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "currency must be a 3-letter currency code")
        String currency
) {
}
