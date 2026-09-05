package com.waypoint.household.web.dto;

import com.waypoint.household.FinancialSnapshot;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record FinancialSnapshotSummaryResponse(
        UUID id,
        LocalDate asOfDate,
        Instant capturedAt
) {

    public static FinancialSnapshotSummaryResponse from(FinancialSnapshot snapshot) {
        return new FinancialSnapshotSummaryResponse(snapshot.getId(), snapshot.getAsOfDate(), snapshot.getCapturedAt());
    }
}
