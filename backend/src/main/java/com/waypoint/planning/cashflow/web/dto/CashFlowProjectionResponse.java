package com.waypoint.planning.cashflow.web.dto;

import com.waypoint.planning.cashflow.CashFlowProjectionResult;
import com.waypoint.planning.cashflow.CashFlowProjectionStatus;
import java.math.BigDecimal;
import java.util.List;

public record CashFlowProjectionResponse(
        String currency,
        String startMonth,
        BigDecimal startingCash,
        BigDecimal monthlyInflow,
        BigDecimal monthlyOutflow,
        int months,
        List<CashFlowProjectionRowResponse> rows,
        BigDecimal endingCash,
        BigDecimal lowestClosingBalance,
        String lowestClosingBalanceMonth,
        String firstNegativeMonth,
        CashFlowProjectionStatus status
) {

    public static CashFlowProjectionResponse from(CashFlowProjectionResult result) {
        return new CashFlowProjectionResponse(
                result.currency(),
                result.startMonth().toString(),
                result.startingCash(),
                result.monthlyInflow(),
                result.monthlyOutflow(),
                result.months(),
                result.rows().stream().map(CashFlowProjectionRowResponse::from).toList(),
                result.endingCash(),
                result.lowestClosingBalance(),
                result.lowestClosingBalanceMonth().toString(),
                result.firstNegativeMonth() == null ? null : result.firstNegativeMonth().toString(),
                result.status()
        );
    }
}
