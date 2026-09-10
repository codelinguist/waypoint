package com.waypoint.household.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record RecordLiabilityBalanceRequest(
        @NotNull(message = "outstandingBalance must not be null")
        @DecimalMin(value = "0", message = "outstandingBalance must not be negative")
        @Digits(integer = 17, fraction = 2, message = "outstandingBalance must have at most 17 integer digits and 2 fraction digits")
        BigDecimal outstandingBalance,

        @NotNull(message = "balanceAsOf must not be null")
        @PastOrPresent(message = "balanceAsOf must not be in the future")
        LocalDate balanceAsOf,

        @NotBlank(message = "reason must not be blank")
        @Size(max = 500, message = "reason must not exceed 500 characters")
        String reason,

        @NotNull(message = "expectedRevision must not be null")
        Long expectedRevision
) {
}
