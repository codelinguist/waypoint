package com.waypoint.household.web.dto;

import com.waypoint.household.FinancialSnapshotComparison;
import java.util.List;

public record FinancialSnapshotComparisonResponse(
        FinancialSnapshotSummaryResponse earlierSnapshot,
        FinancialSnapshotSummaryResponse laterSnapshot,
        List<CurrencyTotalsDeltaResponse> currencyDeltas
) {

    public static FinancialSnapshotComparisonResponse from(FinancialSnapshotComparison comparison) {
        return new FinancialSnapshotComparisonResponse(
                FinancialSnapshotSummaryResponse.from(comparison.earlierSnapshot()),
                FinancialSnapshotSummaryResponse.from(comparison.laterSnapshot()),
                comparison.currencyDeltas().stream().map(CurrencyTotalsDeltaResponse::from).toList()
        );
    }
}
