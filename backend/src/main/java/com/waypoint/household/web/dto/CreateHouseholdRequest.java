package com.waypoint.household.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateHouseholdRequest(
        @NotBlank(message = "name must not be blank")
        String name,

        @NotBlank(message = "baseCurrency must not be blank")
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "baseCurrency must be a 3-letter currency code")
        String baseCurrency
) {
}
