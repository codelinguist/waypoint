package com.waypoint.review.freshness;

import com.waypoint.household.SourceType;
import java.time.LocalDate;
import java.util.UUID;

/**
 * The deterministic freshness classification of one source record against a
 * single reviewDate/maxAgeDays request. {@code ageDays} is signed:
 * {@code reviewDate - sourceDate}, negative when the source date is after
 * reviewDate.
 */
public record FreshnessRecord(
        UUID recordId,
        FreshnessRecordKind recordKind,
        String name,
        String currency,
        SourceType sourceType,
        LocalDate sourceDate,
        long ageDays,
        FreshnessClassification classification
) {
}
