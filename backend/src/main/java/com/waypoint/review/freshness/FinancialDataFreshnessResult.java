package com.waypoint.review.freshness;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The deterministic result of one freshness review over a household's
 * present asset/liability source rows. Nothing here is persisted, and no
 * financial amount is included.
 */
public record FinancialDataFreshnessResult(
        UUID householdId,
        LocalDate reviewDate,
        int maxAgeDays,
        List<FreshnessRecord> records,
        Map<FreshnessRecordKind, Integer> countsByKind,
        Map<FreshnessClassification, Integer> countsByClassification
) {
}
