package com.waypoint.review.planningassumptions;

import com.waypoint.household.SourceType;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One unsuperseded planning assumption version, reduced to only the
 * metadata this review classifies. Deliberately excludes the assumption's
 * {@code value} and {@code notes}: this review signals which beliefs need
 * attention, not what they say, reducing unnecessary financial-data
 * exposure. The full record remains retrievable by {@code assumptionId}
 * through the existing assumption API.
 */
public record PlanningAssumptionReviewSourceRecord(
        UUID assumptionId,
        String name,
        LocalDate reviewDate,
        LocalDate effectiveFrom,
        LocalDate effectiveUntil,
        SourceType sourceType
) {
}
