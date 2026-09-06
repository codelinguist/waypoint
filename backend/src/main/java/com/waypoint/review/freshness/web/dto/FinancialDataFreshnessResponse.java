package com.waypoint.review.freshness.web.dto;

import com.waypoint.review.freshness.FinancialDataFreshnessResult;
import com.waypoint.review.freshness.FreshnessClassification;
import com.waypoint.review.freshness.FreshnessRecordKind;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record FinancialDataFreshnessResponse(
        UUID householdId,
        LocalDate reviewDate,
        int maxAgeDays,
        List<FreshnessRecordResponse> records,
        Map<FreshnessRecordKind, Integer> countsByKind,
        Map<FreshnessClassification, Integer> countsByClassification,
        String modelNote
) {

    private static final String MODEL_NOTE =
            "ageDays is reviewDate minus each record's stored source date (valuedAt for an asset, balanceAsOf "
            + "for a liability), classified against the supplied maxAgeDays threshold. This reviews the "
            + "freshness of present source rows relative to the supplied reviewDate; it is not proof a value "
            + "is still correct today, and it does not reconstruct historical household state as of "
            + "reviewDate.";

    public static FinancialDataFreshnessResponse from(FinancialDataFreshnessResult result) {
        return new FinancialDataFreshnessResponse(
                result.householdId(),
                result.reviewDate(),
                result.maxAgeDays(),
                result.records().stream().map(FreshnessRecordResponse::from).toList(),
                result.countsByKind(),
                result.countsByClassification(),
                MODEL_NOTE);
    }
}
