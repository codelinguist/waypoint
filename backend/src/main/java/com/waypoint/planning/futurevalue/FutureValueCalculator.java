package com.waypoint.planning.futurevalue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * Pure, stateless calculation of a monthly compound-growth projection from an explicit starting
 * principal, an explicit equal monthly contribution, and an explicit nominal annual percentage
 * rate assumption. Callable independently of HTTP, persistence, and any LLM.
 *
 * <p>Model conventions:
 *
 * <ul>
 *   <li>{@code annualRatePercentage} is a nominal annual rate expressed as a percentage (e.g.
 *       {@code 12.00} means 12% per year), converted to a monthly rate by dividing by 12 and by
 *       100. It is never inferred; the caller supplies it as a temporary assumption, not a fact,
 *       guarantee, or recommendation.
 *   <li>Each month, growth accrues on the opening balance at the monthly rate and is rounded to 2
 *       decimal places with HALF_UP before the equal monthly contribution is added at month end:
 *       {@code closing = opening + round(opening * monthlyRate) + contribution}.
 *   <li>All monetary values (balances, growth, contribution) are kept at 2 decimals.
 *   <li>The schedule is bounded to {@value #MAX_HORIZON_MONTHS} months.
 * </ul>
 *
 * <p>This is an illustrative, disposable projection under one explicit assumption, not a
 * forecast, guarantee, or approved financial decision.
 */
@Service
public class FutureValueCalculator {

    static final int MAX_HORIZON_MONTHS = 1200;

    private static final int MONEY_SCALE = 2;
    private static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_UP;
    private static final int MAX_MONEY_INTEGER_DIGITS = 17;
    private static final int MAX_RATE_INTEGER_DIGITS = 3;
    private static final int MAX_RATE_FRACTION_DIGITS = 4;
    private static final int RATE_INTERNAL_SCALE = 12;
    private static final BigDecimal MONTHS_TIMES_HUNDRED = BigDecimal.valueOf(1200);
    private static final Pattern CURRENCY_PATTERN = Pattern.compile("^[A-Za-z]{3}$");

    static final String CONVENTIONS =
            "Nominal annual rate divided by 12 for a monthly rate; growth accrues monthly on the "
                    + "opening balance and is rounded HALF_UP to 2 decimal places, then the equal "
                    + "monthly contribution is added at each month's end. This models one explicit, "
                    + "caller-supplied assumption, not a guaranteed, historical, or recommended return.";

    public FutureValueResult calculate(
            String currency,
            BigDecimal startingPrincipal,
            BigDecimal monthlyContribution,
            BigDecimal annualRatePercentage,
            int projectionMonths
    ) {
        String normalizedCurrency = validateCurrency(currency);
        BigDecimal normalizedPrincipal = validateMoney(startingPrincipal, "startingPrincipal");
        BigDecimal normalizedContribution = validateMoney(monthlyContribution, "monthlyContribution");
        BigDecimal normalizedRate = validateRate(annualRatePercentage);
        validateProjectionMonths(projectionMonths);

        BigDecimal monthlyRate = normalizedRate.divide(MONTHS_TIMES_HUNDRED, RATE_INTERNAL_SCALE, RoundingMode.HALF_UP);

        List<FutureValueRow> rows = new ArrayList<>(projectionMonths);
        BigDecimal opening = normalizedPrincipal;
        BigDecimal totalGrowth = zeroMoney();

        for (int month = 1; month <= projectionMonths; month++) {
            BigDecimal growth = roundMoney(opening.multiply(monthlyRate));
            BigDecimal closing = opening.add(growth).add(normalizedContribution);
            rows.add(new FutureValueRow(month, opening, growth, normalizedContribution, closing));
            totalGrowth = totalGrowth.add(growth);
            opening = closing;
        }

        BigDecimal totalContributed = normalizedPrincipal.add(
                normalizedContribution.multiply(BigDecimal.valueOf(projectionMonths)));

        return new FutureValueResult(
                normalizedCurrency,
                normalizedPrincipal,
                normalizedContribution,
                normalizedRate,
                projectionMonths,
                opening,
                totalContributed,
                totalGrowth,
                CONVENTIONS,
                List.copyOf(rows));
    }

    private static BigDecimal zeroMoney() {
        return BigDecimal.ZERO.setScale(MONEY_SCALE, MONEY_ROUNDING);
    }

    private static BigDecimal roundMoney(BigDecimal value) {
        return value.setScale(MONEY_SCALE, MONEY_ROUNDING);
    }

    private String validateCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            throw new InvalidFutureValueInputException("currency must not be blank");
        }
        String trimmed = currency.trim();
        if (!CURRENCY_PATTERN.matcher(trimmed).matches()) {
            throw new InvalidFutureValueInputException("currency must be a 3-letter currency code");
        }
        return trimmed.toUpperCase(Locale.ROOT);
    }

    private BigDecimal validateMoney(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new InvalidFutureValueInputException(fieldName + " must not be null");
        }
        if (value.signum() < 0) {
            throw new InvalidFutureValueInputException(fieldName + " must not be negative");
        }
        if (value.scale() > MONEY_SCALE) {
            throw new InvalidFutureValueInputException(
                    fieldName + " must have at most " + MONEY_SCALE + " fraction digits");
        }
        int integerDigits = Math.max(value.precision() - value.scale(), 0);
        if (integerDigits > MAX_MONEY_INTEGER_DIGITS) {
            throw new InvalidFutureValueInputException(
                    fieldName + " must have at most " + MAX_MONEY_INTEGER_DIGITS + " integer digits");
        }
        return value.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
    }

    private BigDecimal validateRate(BigDecimal annualRatePercentage) {
        if (annualRatePercentage == null) {
            throw new InvalidFutureValueInputException("annualRatePercentage must not be null");
        }
        if (annualRatePercentage.signum() < 0) {
            throw new InvalidFutureValueInputException("annualRatePercentage must not be negative");
        }
        if (annualRatePercentage.scale() > MAX_RATE_FRACTION_DIGITS) {
            throw new InvalidFutureValueInputException(
                    "annualRatePercentage must have at most " + MAX_RATE_FRACTION_DIGITS + " fraction digits");
        }
        int integerDigits = Math.max(annualRatePercentage.precision() - annualRatePercentage.scale(), 0);
        if (integerDigits > MAX_RATE_INTEGER_DIGITS) {
            throw new InvalidFutureValueInputException(
                    "annualRatePercentage must have at most " + MAX_RATE_INTEGER_DIGITS + " integer digits");
        }
        return annualRatePercentage;
    }

    private void validateProjectionMonths(int projectionMonths) {
        if (projectionMonths < 1 || projectionMonths > MAX_HORIZON_MONTHS) {
            throw new InvalidFutureValueInputException(
                    "projectionMonths must be between 1 and " + MAX_HORIZON_MONTHS);
        }
    }
}
