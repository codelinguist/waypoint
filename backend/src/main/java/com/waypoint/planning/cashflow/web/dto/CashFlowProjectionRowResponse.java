package com.waypoint.planning.cashflow.web.dto;

import com.waypoint.planning.cashflow.CashFlowProjectionRow;
import java.math.BigDecimal;

public record CashFlowProjectionRowResponse(
        String month,
        BigDecimal openingCash,
        BigDecimal inflow,
        BigDecimal outflow,
        BigDecimal netCashFlow,
        BigDecimal closingCash
) {

    public static CashFlowProjectionRowResponse from(CashFlowProjectionRow row) {
        return new CashFlowProjectionRowResponse(
                row.month().toString(),
                row.openingCash(),
                row.inflow(),
                row.outflow(),
                row.netCashFlow(),
                row.closingCash()
        );
    }
}
