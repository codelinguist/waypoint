package com.waypoint.scenarios.incomeinterruption.web.dto;

import com.waypoint.scenarios.incomeinterruption.IncomeInterruptionScenarioResult;
import java.math.BigDecimal;
import java.util.List;

public record IncomeInterruptionScenarioResponse(
        String currency,
        BigDecimal openingReserve,
        BigDecimal normalMonthlyNetIncome,
        BigDecimal interruptedMonthlyNetIncome,
        BigDecimal monthlyExpenses,
        int horizonMonths,
        int interruptionStartMonth,
        int interruptionMonths,
        List<IncomeInterruptionScenarioRowResponse> baselineRows,
        List<IncomeInterruptionScenarioRowResponse> scenarioRows,
        List<BigDecimal> closingDeltas,
        BigDecimal endingCash,
        BigDecimal minimumCash,
        Integer firstNegativeMonth,
        BigDecimal additionalOpeningReserveNeeded
) {

    public static IncomeInterruptionScenarioResponse from(IncomeInterruptionScenarioResult result) {
        return new IncomeInterruptionScenarioResponse(
                result.currency(),
                result.openingReserve(),
                result.normalMonthlyNetIncome(),
                result.interruptedMonthlyNetIncome(),
                result.monthlyExpenses(),
                result.horizonMonths(),
                result.interruptionStartMonth(),
                result.interruptionMonths(),
                result.baselineRows().stream().map(IncomeInterruptionScenarioRowResponse::from).toList(),
                result.scenarioRows().stream().map(IncomeInterruptionScenarioRowResponse::from).toList(),
                result.closingDeltas(),
                result.endingCash(),
                result.minimumCash(),
                result.firstNegativeMonth(),
                result.additionalOpeningReserveNeeded()
        );
    }
}
