package com.waypoint.scenarios.debtprepayment;

import com.waypoint.planning.debtamortization.DebtAmortizationCalculator;
import com.waypoint.planning.debtamortization.DebtAmortizationResult;
import com.waypoint.planning.debtamortization.DebtAmortizationStatus;
import com.waypoint.planning.debtamortization.InvalidDebtAmortizationInputException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pure, stateless comparison of an explicit immediate principal prepayment against continuing the
 * same fixed monthly payment on the un-prepaid balance. Callable independently of HTTP,
 * persistence, and any LLM.
 *
 * <p>Reuses the merged, read-only {@link DebtAmortizationCalculator} twice, with identical
 * currency, monthly interest rate, and monthly payment: once for the un-prepaid {@code baseline}
 * principal, and once for the {@code scenario} principal reduced by {@code immediatePrepayment}.
 * The prepayment is applied before the first month's interest accrues; no fees or penalties are
 * modeled.
 *
 * <p>{@code scenarioTotalCashPaid} is {@code immediatePrepayment} plus the scenario schedule's own
 * {@code totalPaid}, so upfront cash is never omitted from the comparison. Lifetime savings
 * ({@code lifetimeInterestSaved}, {@code payoffMonthsSaved}, {@code lifetimeCashSaved}) are only
 * reported when both paths reach {@code PAID_OFF}; a {@code NON_AMORTIZING} or
 * {@code HORIZON_LIMIT} path never has its truncated or absent total compared against a lifetime
 * total.
 */
public final class DebtPrepaymentComparisonCalculator {

    private static final int MONEY_SCALE = 2;
    private static final int MAX_MONEY_INTEGER_DIGITS = 17;

    private DebtPrepaymentComparisonCalculator() {
    }

    public static DebtPrepaymentComparisonResult calculate(
            BigDecimal principal,
            BigDecimal monthlyInterestRate,
            BigDecimal monthlyPayment,
            String currency,
            BigDecimal immediatePrepayment
    ) {
        validateImmediatePrepayment(immediatePrepayment);

        DebtAmortizationResult baseline = calculateAmortization(principal, monthlyInterestRate, monthlyPayment, currency);

        BigDecimal normalizedPrepayment = immediatePrepayment.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
        if (normalizedPrepayment.compareTo(baseline.principal()) > 0) {
            throw new InvalidDebtPrepaymentInputException("immediatePrepayment must not exceed principal");
        }

        BigDecimal scenarioPrincipal = baseline.principal().subtract(normalizedPrepayment);
        DebtAmortizationResult scenario =
                calculateAmortization(scenarioPrincipal, monthlyInterestRate, monthlyPayment, currency);

        BigDecimal scenarioTotalCashPaid = normalizedPrepayment.add(scenario.totalPaid());

        BigDecimal lifetimeInterestSaved = null;
        Integer payoffMonthsSaved = null;
        BigDecimal lifetimeCashSaved = null;
        String comparisonUnavailableReason = null;

        if (baseline.status() == DebtAmortizationStatus.PAID_OFF && scenario.status() == DebtAmortizationStatus.PAID_OFF) {
            lifetimeInterestSaved = baseline.totalInterest().subtract(scenario.totalInterest());
            payoffMonthsSaved = baseline.payoffMonths() - scenario.payoffMonths();
            lifetimeCashSaved = baseline.totalPaid().subtract(scenarioTotalCashPaid);
        } else {
            comparisonUnavailableReason = comparisonUnavailableReason(baseline.status(), scenario.status());
        }

        return new DebtPrepaymentComparisonResult(
                baseline.principal(),
                monthlyInterestRate,
                monthlyPayment,
                baseline.currency(),
                normalizedPrepayment,
                baseline,
                scenario,
                scenarioTotalCashPaid,
                lifetimeInterestSaved,
                payoffMonthsSaved,
                lifetimeCashSaved,
                comparisonUnavailableReason);
    }

    private static DebtAmortizationResult calculateAmortization(
            BigDecimal principal, BigDecimal monthlyInterestRate, BigDecimal monthlyPayment, String currency
    ) {
        try {
            return DebtAmortizationCalculator.calculate(principal, monthlyInterestRate, monthlyPayment, currency);
        } catch (InvalidDebtAmortizationInputException ex) {
            throw new InvalidDebtPrepaymentInputException(ex.getMessage());
        }
    }

    private static String comparisonUnavailableReason(
            DebtAmortizationStatus baselineStatus, DebtAmortizationStatus scenarioStatus
    ) {
        boolean baselinePaidOff = baselineStatus == DebtAmortizationStatus.PAID_OFF;
        boolean scenarioPaidOff = scenarioStatus == DebtAmortizationStatus.PAID_OFF;
        if (!baselinePaidOff && !scenarioPaidOff) {
            return "Neither the baseline (" + baselineStatus + ") nor the scenario (" + scenarioStatus
                    + ") path reaches PAID_OFF, so lifetime savings cannot be compared.";
        }
        if (!baselinePaidOff) {
            return "The baseline path does not reach PAID_OFF (" + baselineStatus
                    + "), so its truncated or absent total cannot be compared against the scenario's lifetime total.";
        }
        return "The scenario path does not reach PAID_OFF (" + scenarioStatus
                + "), so its truncated or absent total cannot be compared against the baseline's lifetime total.";
    }

    private static void validateImmediatePrepayment(BigDecimal immediatePrepayment) {
        if (immediatePrepayment == null) {
            throw new InvalidDebtPrepaymentInputException("immediatePrepayment must not be null");
        }
        if (immediatePrepayment.signum() < 0) {
            throw new InvalidDebtPrepaymentInputException("immediatePrepayment must not be negative");
        }
        if (immediatePrepayment.scale() > MONEY_SCALE) {
            throw new InvalidDebtPrepaymentInputException(
                    "immediatePrepayment must have at most " + MONEY_SCALE + " fractional digits");
        }
        int integerDigits = Math.max(immediatePrepayment.precision() - immediatePrepayment.scale(), 0);
        if (integerDigits > MAX_MONEY_INTEGER_DIGITS) {
            throw new InvalidDebtPrepaymentInputException(
                    "immediatePrepayment must have at most " + MAX_MONEY_INTEGER_DIGITS + " integer digits");
        }
    }
}
