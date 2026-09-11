package com.waypoint.review.planningassumptions;

import com.waypoint.household.SourceType;
import java.time.LocalDate;
import java.util.UUID;

/**
 * The deterministic classification of one unsuperseded planning assumption
 * against a single {@code asOf} request. {@code reviewStatus} and
 * {@code effectiveStatus} are independent dimensions — either can need
 * attention while the other does not.
 */
public record PlanningAssumptionReviewRow(
        UUID assumptionId,
        String name,
        LocalDate reviewDate,
        LocalDate effectiveFrom,
        LocalDate effectiveUntil,
        SourceType sourceType,
        ReviewStatus reviewStatus,
        EffectiveStatus effectiveStatus,
        boolean needsAttention
) {
}
