package com.waypoint.household.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;

public record CreateFinancialSnapshotRequest(
        @NotNull(message = "asOfDate must not be null")
        @PastOrPresent(message = "asOfDate must not be in the future")
        LocalDate asOfDate
) {
}
