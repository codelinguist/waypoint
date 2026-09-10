package com.waypoint.scenarios.debtprepayment.web.dto;

import com.waypoint.scenarios.debtprepayment.DebtPrepaymentComparisonResult;
import java.math.BigDecimal;

public record DebtPrepaymentComparisonResponse(
        BigDecimal principal,
        BigDecimal monthlyInterestRate,
        BigDecimal monthlyPayment,
        String currency,
        BigDecimal immediatePrepayment,
        DebtPrepaymentPathResponse baseline,
        DebtPrepaymentPathResponse scenario,
        BigDecimal scenarioTotalCashPaid,
        BigDecimal lifetimeInterestSaved,
        Integer payoffMonthsSaved,
        BigDecimal lifetimeCashSaved,
        String comparisonUnavailableReason
) {

    public static DebtPrepaymentComparisonResponse from(DebtPrepaymentComparisonResult result) {
        return new DebtPrepaymentComparisonResponse(
                result.principal(),
                result.monthlyInterestRate(),
                result.monthlyPayment(),
                result.currency(),
                result.immediatePrepayment(),
                DebtPrepaymentPathResponse.from(result.baseline()),
                DebtPrepaymentPathResponse.from(result.scenario()),
                result.scenarioTotalCashPaid(),
                result.lifetimeInterestSaved(),
                result.payoffMonthsSaved(),
                result.lifetimeCashSaved(),
                result.comparisonUnavailableReason()
        );
    }
}
