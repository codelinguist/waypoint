package com.waypoint.household.web.dto;

import com.waypoint.household.AssetType;
import com.waypoint.household.Liquidity;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateAssetRequest(
        @NotBlank(message = "name must not be blank")
        String name,

        @NotNull(message = "assetType must not be null")
        AssetType assetType,

        @NotNull(message = "estimatedValue must not be null")
        @DecimalMin(value = "0", message = "estimatedValue must not be negative")
        BigDecimal estimatedValue,

        @NotNull(message = "planningValue must not be null")
        @DecimalMin(value = "0", message = "planningValue must not be negative")
        BigDecimal planningValue,

        @NotBlank(message = "currency must not be blank")
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "currency must be a 3-letter currency code")
        String currency,

        @NotNull(message = "valuedAt must not be null")
        @PastOrPresent(message = "valuedAt must not be in the future")
        LocalDate valuedAt,

        @NotNull(message = "liquidity must not be null")
        Liquidity liquidity
) {
}
