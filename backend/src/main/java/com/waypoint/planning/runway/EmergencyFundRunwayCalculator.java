package com.waypoint.planning.runway;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * Pure, stateless calculation of how long an explicitly supplied cash
 * reserve covers an explicitly supplied monthly funding shortfall, from
 * caller-supplied temporary inputs only. Callable directly, independently
 * of HTTP, persistence, and an LLM; it enforces its own input invariants
 * rather than trusting transport-layer validation alone.
 *
 * <p>{@code runwayMonths} and {@code fullMonthsCovered} are both derived
 * from the unrounded {@code availableReserve / monthlyShortfall} ratio using
 * exact integer arithmetic on cent-scaled amounts, so neither value is
 * corrupted by an intermediate rounding step.
 */
@Service
public class EmergencyFundRunwayCalculator {

    private static final Pattern CURRENCY_PATTERN = Pattern.compile("^[A-Za-z]{3}$");
    private static final int MAX_INTEGER_DIGITS = 17;
    private static final int SCALE = 2;
    private static final BigInteger HUNDRED = BigInteger.valueOf(100);

    public EmergencyFundRunwayResult calculate(
            BigDecimal availableReserve,
            BigDecimal monthlyExpenses,
            BigDecimal monthlyNetIncome,
            String currency
    ) {
        String normalizedCurrency = normalizeCurrency(currency);
        validateAmount(availableReserve, "availableReserve");
        validateAmount(monthlyExpenses, "monthlyExpenses");
        validateAmount(monthlyNetIncome, "monthlyNetIncome");

        BigDecimal rawShortfall = monthlyExpenses.subtract(monthlyNetIncome);
        BigDecimal monthlyShortfall = (rawShortfall.signum() > 0 ? rawShortfall : BigDecimal.ZERO)
                .setScale(SCALE, RoundingMode.UNNECESSARY);

        if (monthlyShortfall.signum() == 0) {
            return new EmergencyFundRunwayResult(
                    normalizedCurrency,
                    availableReserve,
                    monthlyExpenses,
                    monthlyNetIncome,
                    monthlyShortfall,
                    RunwayStatus.NO_SHORTFALL,
                    null,
                    null);
        }

        BigInteger reserveCents = availableReserve.movePointRight(SCALE).toBigIntegerExact();
        BigInteger shortfallCents = monthlyShortfall.movePointRight(SCALE).toBigIntegerExact();

        BigInteger fullMonthsCovered = reserveCents.divide(shortfallCents);
        BigInteger hundredthsOfAMonth = reserveCents.multiply(HUNDRED).divide(shortfallCents);
        BigDecimal runwayMonths = new BigDecimal(hundredthsOfAMonth, SCALE);

        return new EmergencyFundRunwayResult(
                normalizedCurrency,
                availableReserve,
                monthlyExpenses,
                monthlyNetIncome,
                monthlyShortfall,
                RunwayStatus.FINITE,
                runwayMonths,
                fullMonthsCovered);
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            throw new InvalidRunwayInputException("currency must not be blank");
        }
        String trimmed = currency.trim();
        if (!CURRENCY_PATTERN.matcher(trimmed).matches()) {
            throw new InvalidRunwayInputException("currency must be a 3-letter currency code");
        }
        return trimmed.toUpperCase(Locale.ROOT);
    }

    private void validateAmount(BigDecimal amount, String fieldName) {
        if (amount == null) {
            throw new InvalidRunwayInputException(fieldName + " must not be null");
        }
        if (amount.signum() < 0) {
            throw new InvalidRunwayInputException(fieldName + " must not be negative");
        }
        if (amount.scale() > SCALE) {
            throw new InvalidRunwayInputException(fieldName + " must have at most " + SCALE + " fraction digits");
        }
        int integerDigits = amount.precision() - amount.scale();
        if (integerDigits > MAX_INTEGER_DIGITS) {
            throw new InvalidRunwayInputException(
                    fieldName + " must have at most " + MAX_INTEGER_DIGITS + " integer digits");
        }
    }
}
