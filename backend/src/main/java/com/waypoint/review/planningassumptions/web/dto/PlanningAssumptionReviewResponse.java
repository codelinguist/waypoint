package com.waypoint.review.planningassumptions.web.dto;

import com.waypoint.review.planningassumptions.EffectiveStatus;
import com.waypoint.review.planningassumptions.PlanningAssumptionReviewResult;
import com.waypoint.review.planningassumptions.ReviewStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PlanningAssumptionReviewResponse(
        UUID householdId,
        LocalDate asOf,
        List<PlanningAssumptionReviewRowResponse> rows,
        int totalCount,
        int needsAttentionCount,
        Map<ReviewStatus, Integer> countsByReviewStatus,
        Map<EffectiveStatus, Integer> countsByEffectiveStatus,
        String modelNote
) {

    private static final String MODEL_NOTE =
            "This reviews the household's current unsuperseded planning-assumption versions as of the supplied "
            + "asOf date; it does not reconstruct which beliefs were current as of a past date. reviewStatus and "
            + "effectiveStatus are independent — a future-effective assumption can still be due for review, and "
            + "an expired assumption can still have an upcoming review date.";

    public static PlanningAssumptionReviewResponse from(PlanningAssumptionReviewResult result) {
        return new PlanningAssumptionReviewResponse(
                result.householdId(),
                result.asOf(),
                result.rows().stream().map(PlanningAssumptionReviewRowResponse::from).toList(),
                result.totalCount(),
                result.needsAttentionCount(),
                result.countsByReviewStatus(),
                result.countsByEffectiveStatus(),
                MODEL_NOTE);
    }
}
