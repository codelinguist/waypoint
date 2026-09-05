package com.waypoint.household.web.dto;

import com.waypoint.household.PlanVersusActualAnalysis;
import java.util.List;

public record PlanVersusActualResponse(
        FinancialSnapshotSummaryResponse snapshot,
        List<CurrencyPlanVersusActualResponse> currencyResults
) {

    public static PlanVersusActualResponse from(PlanVersusActualAnalysis analysis) {
        return new PlanVersusActualResponse(
                FinancialSnapshotSummaryResponse.from(analysis.snapshot()),
                analysis.currencyResults().stream().map(CurrencyPlanVersusActualResponse::from).toList()
        );
    }
}
