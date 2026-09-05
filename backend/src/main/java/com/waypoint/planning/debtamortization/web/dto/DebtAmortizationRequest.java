package com.waypoint.planning.debtamortization.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public record DebtAmortizationRequest(
        @NotNull(message = "principal must not be null")
        @DecimalMin(value = "0", message = "principal must not be negative")
        @Digits(integer = 17, fraction = 2, message = "principal must have at most 17 integer digits and 2 fraction digits")
        BigDecimal principal,

        @NotNull(message = "monthlyInterestRate must not be null")
        @DecimalMin(value = "0", message = "monthlyInterestRate must not be negative")
        @DecimalMax(value = "1", message = "monthlyInterestRate must not exceed 1")
        @Digits(integer = 1, fraction = 8, message = "monthlyInterestRate must have at most 1 integer digit and 8 fraction digits")
        BigDecimal monthlyInterestRate,

        @NotNull(message = "monthlyPayment must not be null")
        @DecimalMin(value = "0", inclusive = false, message = "monthlyPayment must be greater than 0")
        @Digits(integer = 17, fraction = 2, message = "monthlyPayment must have at most 17 integer digits and 2 fraction digits")
        BigDecimal monthlyPayment,

        @NotBlank(message = "currency must not be blank")
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "currency must be a 3-letter currency code")
        String currency
) {
}
