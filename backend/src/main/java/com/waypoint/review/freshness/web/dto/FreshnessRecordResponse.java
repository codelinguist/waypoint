package com.waypoint.review.freshness.web.dto;

import com.waypoint.household.SourceType;
import com.waypoint.review.freshness.FreshnessClassification;
import com.waypoint.review.freshness.FreshnessRecord;
import com.waypoint.review.freshness.FreshnessRecordKind;
import java.time.LocalDate;
import java.util.UUID;

public record FreshnessRecordResponse(
        UUID recordId,
        FreshnessRecordKind recordKind,
        String name,
        String currency,
        SourceType sourceType,
        LocalDate sourceDate,
        long ageDays,
        FreshnessClassification classification
) {
    public static FreshnessRecordResponse from(FreshnessRecord record) {
        return new FreshnessRecordResponse(
                record.recordId(),
                record.recordKind(),
                record.name(),
                record.currency(),
                record.sourceType(),
                record.sourceDate(),
                record.ageDays(),
                record.classification());
    }
}
