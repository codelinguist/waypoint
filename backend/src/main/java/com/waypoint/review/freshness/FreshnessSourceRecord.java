package com.waypoint.review.freshness;

import com.waypoint.household.SourceType;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One current asset or liability row, reduced to only the metadata this
 * review needs. Deliberately excludes any financial amount: this feature
 * never copies a value into its output.
 */
public record FreshnessSourceRecord(
        UUID recordId,
        FreshnessRecordKind recordKind,
        String name,
        String currency,
        SourceType sourceType,
        LocalDate sourceDate
) {
}
