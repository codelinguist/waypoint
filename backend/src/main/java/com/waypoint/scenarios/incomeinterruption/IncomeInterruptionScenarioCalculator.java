package com.waypoint.scenarios.incomeinterruption;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * Determines the reserve needed to withstand a caller-defined temporary income interruption by
 * comparing two explicit monthly paths from the same opening reserve and expenses: a baseline
 * that keeps {@code normalMonthlyNetIncome} throughout, and a scenario that substitutes
 * {@code interruptedMonthlyNetIncome} for an inclusive interval of months before restoring normal
 * income.
 *
 * <p>This is a disposable, stateless calculation over caller-supplied temporary inputs: it uses
 * explicit month indices rather than inferred employment dates, ignores timing within a month,
 * and does not read or write any persisted household state. It never establishes an actual
 * household cash-flow forecast, recommendation, or approved decision. It enforces its own input
 * invariants so it rejects invalid values whether it is called directly or through the HTTP
 * layer, which applies the same rules independently via request validation.
 *
 * <p>Every monetary amount is kept at exactly 2 decimal places. Since a month's balance is
 * derived only by addition and subtraction of already-2-decimal values, no rounding is ever
 * applied to a derived balance; {@link BigDecimal} arithmetic also never overflows the way a
 * primitive numeric type could across up to {@value #MAX_HORIZON_MONTHS} months.
 */
@Service
public class IncomeInterruptionScenarioCalculator {

    static final int MIN_HORIZON_MONTHS = 1;
    static final int MAX_HORIZON_MONTHS = 1200;

    private static final int MONEY_SCALE = 2;
    private static final int MAX_MONEY_INTEGER_DIGITS = 17;
    private static final Pattern CURRENCY_CODE = Pattern.compile("^[A-Za-z]{3}$");

    public IncomeInterruptionScenarioResult calculate(
            String currency,
            BigDecimal openingReserve,
            BigDecimal normalMonthlyNetIncome,
            BigDecimal interruptedMonthlyNetIncome,
            BigDecimal monthlyExpenses,
            int horizonMonths,
            int interruptionStartMonth,
            int interruptionMonths
    ) {
        String normalizedCurrency = validateCurrency(currency);
        BigDecimal normalizedOpeningReserve = validateMoney(openingReserve, "openingReserve");
        BigDecimal normalizedNormalIncome = validateMoney(normalMonthlyNetIncome, "normalMonthlyNetIncome");
        BigDecimal normalizedInterruptedIncome =
                validateMoney(interruptedMonthlyNetIncome, "interruptedMonthlyNetIncome");
        BigDecimal normalizedExpenses = validateMoney(monthlyExpenses, "monthlyExpenses");
        if (normalizedInterruptedIncome.compareTo(normalizedNormalIncome) > 0) {
            throw new InvalidIncomeInterruptionScenarioInputException(
                    "interruptedMonthlyNetIncome must not exceed normalMonthlyNetIncome");
        }
        validateHorizonMonths(horizonMonths);
        validateWithinHorizon(interruptionStartMonth, horizonMonths, "interruptionStartMonth");
        validateWithinHorizon(interruptionMonths, horizonMonths, "interruptionMonths");
        int interruptionEndMonth = interruptionStartMonth + interruptionMonths - 1;
        if (interruptionEndMonth > horizonMonths) {
            throw new InvalidIncomeInterruptionScenarioInputException(
                    "interruptionStartMonth and interruptionMonths must describe an interval that fits within horizonMonths");
        }

        List<IncomeInterruptionScenarioRow> baselineRows = new ArrayList<>(horizonMonths);
        List<IncomeInterruptionScenarioRow> scenarioRows = new ArrayList<>(horizonMonths);
        List<BigDecimal> closingDeltas = new ArrayList<>(horizonMonths);

        BigDecimal baselineOpening = normalizedOpeningReserve;
        BigDecimal scenarioOpening = normalizedOpeningReserve;
        BigDecimal minimumCash = normalizedOpeningReserve;
        Integer firstNegativeMonth = null;

        for (int month = 1; month <= horizonMonths; month++) {
            boolean interrupted = month >= interruptionStartMonth && month <= interruptionEndMonth;

            BigDecimal baselineNetCashFlow = normalizedNormalIncome.subtract(normalizedExpenses);
            BigDecimal baselineClosing = baselineOpening.add(baselineNetCashFlow);
            baselineRows.add(new IncomeInterruptionScenarioRow(
                    month, baselineOpening, normalizedNormalIncome, normalizedExpenses,
                    baselineNetCashFlow, baselineClosing));

            BigDecimal scenarioIncome = interrupted ? normalizedInterruptedIncome : normalizedNormalIncome;
            BigDecimal scenarioNetCashFlow = scenarioIncome.subtract(normalizedExpenses);
            BigDecimal scenarioClosing = scenarioOpening.add(scenarioNetCashFlow);
            scenarioRows.add(new IncomeInterruptionScenarioRow(
                    month, scenarioOpening, scenarioIncome, normalizedExpenses,
                    scenarioNetCashFlow, scenarioClosing));

            closingDeltas.add(scenarioClosing.subtract(baselineClosing));

            if (scenarioClosing.compareTo(minimumCash) < 0) {
                minimumCash = scenarioClosing;
            }
            if (firstNegativeMonth == null && scenarioClosing.signum() < 0) {
                firstNegativeMonth = month;
            }

            baselineOpening = baselineClosing;
            scenarioOpening = scenarioClosing;
        }

        BigDecimal endingCash = scenarioOpening;
        BigDecimal additionalOpeningReserveNeeded = minimumCash.signum() < 0
                ? minimumCash.negate()
                : BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);

        return new IncomeInterruptionScenarioResult(
                normalizedCurrency,
                normalizedOpeningReserve,
                normalizedNormalIncome,
                normalizedInterruptedIncome,
                normalizedExpenses,
                horizonMonths,
                interruptionStartMonth,
                interruptionMonths,
                List.copyOf(baselineRows),
                List.copyOf(scenarioRows),
                List.copyOf(closingDeltas),
                endingCash,
                minimumCash,
                firstNegativeMonth,
                additionalOpeningReserveNeeded
        );
    }

    private String validateCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            throw new InvalidIncomeInterruptionScenarioInputException("currency must not be blank");
        }
        if (!CURRENCY_CODE.matcher(currency).matches()) {
            throw new InvalidIncomeInterruptionScenarioInputException("currency must be a 3-letter currency code");
        }
        return currency.toUpperCase(Locale.ROOT);
    }

    private BigDecimal validateMoney(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new InvalidIncomeInterruptionScenarioInputException(fieldName + " must not be null");
        }
        if (value.scale() > MONEY_SCALE) {
            throw new InvalidIncomeInterruptionScenarioInputException(
                    fieldName + " must have at most " + MONEY_SCALE + " fraction digits");
        }
        int integerDigits = Math.max(value.precision() - value.scale(), 0);
        if (integerDigits > MAX_MONEY_INTEGER_DIGITS) {
            throw new InvalidIncomeInterruptionScenarioInputException(
                    fieldName + " must have at most " + MAX_MONEY_INTEGER_DIGITS + " integer digits");
        }
        if (value.signum() < 0) {
            throw new InvalidIncomeInterruptionScenarioInputException(fieldName + " must not be negative");
        }
        return value.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
    }

    private void validateHorizonMonths(int horizonMonths) {
        if (horizonMonths < MIN_HORIZON_MONTHS || horizonMonths > MAX_HORIZON_MONTHS) {
            throw new InvalidIncomeInterruptionScenarioInputException(
                    "horizonMonths must be between " + MIN_HORIZON_MONTHS + " and " + MAX_HORIZON_MONTHS);
        }
    }

    private void validateWithinHorizon(int value, int horizonMonths, String fieldName) {
        if (value < MIN_HORIZON_MONTHS || value > horizonMonths) {
            throw new InvalidIncomeInterruptionScenarioInputException(
                    fieldName + " must be between 1 and horizonMonths (" + horizonMonths + ")");
        }
    }
}
