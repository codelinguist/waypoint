package com.waypoint.household.web.dto;

import com.waypoint.household.PlannedCurrencyTotals;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public record PlannedCurrencyTotalsRequest(
        @NotBlank(message = "currency must not be blank")
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "currency must be a 3-letter currency code")
        String currency,

        @NotNull(message = "assetTotal must not be null")
        @DecimalMin(value = "0", message = "assetTotal must not be negative")
        @Digits(integer = 17, fraction = 2, message = "assetTotal must have at most 17 integer digits and 2 fraction digits")
        BigDecimal assetTotal,

        @NotNull(message = "liabilityTotal must not be null")
        @DecimalMin(value = "0", message = "liabilityTotal must not be negative")
        @Digits(integer = 17, fraction = 2, message = "liabilityTotal must have at most 17 integer digits and 2 fraction digits")
        BigDecimal liabilityTotal,

        @NotNull(message = "netWorth must not be null")
        @Digits(integer = 17, fraction = 2, message = "netWorth must have at most 17 integer digits and 2 fraction digits")
        BigDecimal netWorth
) {

    public PlannedCurrencyTotals toDomain() {
        return new PlannedCurrencyTotals(currency, assetTotal, liabilityTotal, netWorth);
    }
}
