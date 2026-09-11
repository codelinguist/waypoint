package com.waypoint.review.planningassumptions.web.dto;

import com.waypoint.household.SourceType;
import com.waypoint.review.planningassumptions.EffectiveStatus;
import com.waypoint.review.planningassumptions.PlanningAssumptionReviewRow;
import com.waypoint.review.planningassumptions.ReviewStatus;
import java.time.LocalDate;
import java.util.UUID;

public record PlanningAssumptionReviewRowResponse(
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
    public static PlanningAssumptionReviewRowResponse from(PlanningAssumptionReviewRow row) {
        return new PlanningAssumptionReviewRowResponse(
                row.assumptionId(),
                row.name(),
                row.reviewDate(),
                row.effectiveFrom(),
                row.effectiveUntil(),
                row.sourceType(),
                row.reviewStatus(),
                row.effectiveStatus(),
                row.needsAttention());
    }
}
