package com.waypoint.planning.futurevalue.web.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public record FutureValueRequest(
        @NotBlank(message = "currency must not be blank")
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "currency must be a 3-letter currency code")
        String currency,

        @NotNull(message = "startingPrincipal must not be null")
        @DecimalMin(value = "0", message = "startingPrincipal must not be negative")
        @Digits(integer = 17, fraction = 2, message = "startingPrincipal must have at most 17 integer digits and 2 fraction digits")
        BigDecimal startingPrincipal,

        @NotNull(message = "monthlyContribution must not be null")
        @DecimalMin(value = "0", message = "monthlyContribution must not be negative")
        @Digits(integer = 17, fraction = 2, message = "monthlyContribution must have at most 17 integer digits and 2 fraction digits")
        BigDecimal monthlyContribution,

        @NotNull(message = "annualRatePercentage must not be null")
        @DecimalMin(value = "0", message = "annualRatePercentage must not be negative")
        @Digits(integer = 3, fraction = 4, message = "annualRatePercentage must have at most 3 integer digits and 4 fraction digits")
        BigDecimal annualRatePercentage,

        @NotNull(message = "projectionMonths must not be null")
        @Min(value = 1, message = "projectionMonths must be at least 1")
        @Max(value = 1200, message = "projectionMonths must be at most 1200")
        @JsonDeserialize(using = WholeNumberDeserializer.class)
        Integer projectionMonths
) {
}
