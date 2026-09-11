package com.waypoint.review.planningassumptions;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The deterministic result of one review over a household's present
 * unsuperseded planning assumptions. Nothing here is persisted, and no
 * assumption value or note is included. {@code totalCount} and
 * {@code needsAttentionCount} are derived from exactly {@code rows}.
 */
public record PlanningAssumptionReviewResult(
        UUID householdId,
        LocalDate asOf,
        List<PlanningAssumptionReviewRow> rows,
        int totalCount,
        int needsAttentionCount,
        Map<ReviewStatus, Integer> countsByReviewStatus,
        Map<EffectiveStatus, Integer> countsByEffectiveStatus
) {
}
