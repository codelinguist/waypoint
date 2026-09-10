package com.waypoint.scenarios.incomeinterruption.web.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public record IncomeInterruptionScenarioRequest(
        @NotBlank(message = "currency must not be blank")
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "currency must be a 3-letter currency code")
        String currency,

        @NotNull(message = "openingReserve must not be null")
        @DecimalMin(value = "0", message = "openingReserve must not be negative")
        @Digits(integer = 17, fraction = 2, message = "openingReserve must have at most 17 integer digits and 2 fraction digits")
        BigDecimal openingReserve,

        @NotNull(message = "normalMonthlyNetIncome must not be null")
        @DecimalMin(value = "0", message = "normalMonthlyNetIncome must not be negative")
        @Digits(integer = 17, fraction = 2, message = "normalMonthlyNetIncome must have at most 17 integer digits and 2 fraction digits")
        BigDecimal normalMonthlyNetIncome,

        @NotNull(message = "interruptedMonthlyNetIncome must not be null")
        @DecimalMin(value = "0", message = "interruptedMonthlyNetIncome must not be negative")
        @Digits(integer = 17, fraction = 2, message = "interruptedMonthlyNetIncome must have at most 17 integer digits and 2 fraction digits")
        BigDecimal interruptedMonthlyNetIncome,

        @NotNull(message = "monthlyExpenses must not be null")
        @DecimalMin(value = "0", message = "monthlyExpenses must not be negative")
        @Digits(integer = 17, fraction = 2, message = "monthlyExpenses must have at most 17 integer digits and 2 fraction digits")
        BigDecimal monthlyExpenses,

        @NotNull(message = "horizonMonths must not be null")
        @Min(value = 1, message = "horizonMonths must be at least 1")
        @Max(value = 1200, message = "horizonMonths must be at most 1200")
        @JsonDeserialize(using = WholeNumberDeserializer.class)
        Integer horizonMonths,

        @NotNull(message = "interruptionStartMonth must not be null")
        @Min(value = 1, message = "interruptionStartMonth must be at least 1")
        @Max(value = 1200, message = "interruptionStartMonth must be at most 1200")
        @JsonDeserialize(using = WholeNumberDeserializer.class)
        Integer interruptionStartMonth,

        @NotNull(message = "interruptionMonths must not be null")
        @Min(value = 1, message = "interruptionMonths must be at least 1")
        @Max(value = 1200, message = "interruptionMonths must be at most 1200")
        @JsonDeserialize(using = WholeNumberDeserializer.class)
        Integer interruptionMonths
) {
}
