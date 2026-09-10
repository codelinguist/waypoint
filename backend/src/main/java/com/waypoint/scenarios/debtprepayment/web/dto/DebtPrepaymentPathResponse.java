package com.waypoint.scenarios.debtprepayment.web.dto;

import com.waypoint.planning.debtamortization.DebtAmortizationResult;
import java.math.BigDecimal;
import java.util.List;

/**
 * One side (baseline or scenario) of a debt prepayment comparison, mapped from the reused
 * {@link DebtAmortizationResult}.
 */
public record DebtPrepaymentPathResponse(
        BigDecimal startingBalance,
        String status,
        Integer payoffMonths,
        BigDecimal totalPaid,
        BigDecimal totalInterest,
        BigDecimal remainingBalance,
        List<DebtPrepaymentScheduleRowResponse> schedule
) {

    public static DebtPrepaymentPathResponse from(DebtAmortizationResult result) {
        return new DebtPrepaymentPathResponse(
                result.principal(),
                result.status().name(),
                result.payoffMonths(),
                result.totalPaid(),
                result.totalInterest(),
                result.remainingBalance(),
                result.schedule().stream().map(DebtPrepaymentScheduleRowResponse::from).toList()
        );
    }
}
