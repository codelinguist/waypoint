package com.waypoint.scenarios.incomeinterruption.web.dto;

import com.waypoint.scenarios.incomeinterruption.IncomeInterruptionScenarioRow;
import java.math.BigDecimal;

public record IncomeInterruptionScenarioRowResponse(
        int month,
        BigDecimal openingCash,
        BigDecimal income,
        BigDecimal expenses,
        BigDecimal netCashFlow,
        BigDecimal closingCash
) {

    public static IncomeInterruptionScenarioRowResponse from(IncomeInterruptionScenarioRow row) {
        return new IncomeInterruptionScenarioRowResponse(
                row.month(),
                row.openingCash(),
                row.income(),
                row.expenses(),
                row.netCashFlow(),
                row.closingCash()
        );
    }
}
