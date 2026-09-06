package com.waypoint.planning.cashflow.web.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public record CashFlowProjectionRequest(
        @NotBlank(message = "currency must not be blank")
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "currency must be a 3-letter currency code")
        String currency,

        @NotBlank(message = "startMonth must not be blank")
        @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "startMonth must be in YYYY-MM format")
        String startMonth,

        @NotNull(message = "startingCash must not be null")
        @DecimalMin(value = "0", message = "startingCash must not be negative")
        @Digits(integer = 17, fraction = 2, message = "startingCash must have at most 17 integer digits and 2 fraction digits")
        BigDecimal startingCash,

        @NotNull(message = "monthlyInflow must not be null")
        @DecimalMin(value = "0", message = "monthlyInflow must not be negative")
        @Digits(integer = 17, fraction = 2, message = "monthlyInflow must have at most 17 integer digits and 2 fraction digits")
        BigDecimal monthlyInflow,

        @NotNull(message = "monthlyOutflow must not be null")
        @DecimalMin(value = "0", message = "monthlyOutflow must not be negative")
        @Digits(integer = 17, fraction = 2, message = "monthlyOutflow must have at most 17 integer digits and 2 fraction digits")
        BigDecimal monthlyOutflow,

        @NotNull(message = "months must not be null")
        @Min(value = 1, message = "months must be at least 1")
        @Max(value = 1200, message = "months must be at most 1200")
        @JsonDeserialize(using = WholeNumberDeserializer.class)
        Integer months
) {
}
