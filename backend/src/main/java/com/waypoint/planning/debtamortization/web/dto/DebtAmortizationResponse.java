package com.waypoint.planning.debtamortization.web.dto;

import com.waypoint.planning.debtamortization.DebtAmortizationResult;
import java.math.BigDecimal;
import java.util.List;

public record DebtAmortizationResponse(
        BigDecimal principal,
        BigDecimal monthlyInterestRate,
        BigDecimal monthlyPayment,
        String currency,
        String status,
        Integer payoffMonths,
        BigDecimal totalPaid,
        BigDecimal totalInterest,
        BigDecimal remainingBalance,
        List<DebtAmortizationRowResponse> schedule
) {

    public static DebtAmortizationResponse from(DebtAmortizationResult result) {
        return new DebtAmortizationResponse(
                result.principal(),
                result.monthlyInterestRate(),
                result.monthlyPayment(),
                result.currency(),
                result.status().name(),
                result.payoffMonths(),
                result.totalPaid(),
                result.totalInterest(),
                result.remainingBalance(),
                result.schedule().stream().map(DebtAmortizationRowResponse::from).toList()
        );
    }
}
