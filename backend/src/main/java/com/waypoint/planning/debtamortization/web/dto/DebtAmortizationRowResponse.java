package com.waypoint.planning.debtamortization.web.dto;

import com.waypoint.planning.debtamortization.DebtAmortizationRow;
import java.math.BigDecimal;

public record DebtAmortizationRowResponse(
        int month,
        BigDecimal openingBalance,
        BigDecimal interest,
        BigDecimal payment,
        BigDecimal principalRepaid,
        BigDecimal closingBalance
) {

    public static DebtAmortizationRowResponse from(DebtAmortizationRow row) {
        return new DebtAmortizationRowResponse(
                row.month(),
                row.openingBalance(),
                row.interest(),
                row.payment(),
                row.principalRepaid(),
                row.closingBalance()
        );
    }
}
