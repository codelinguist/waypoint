package com.waypoint.planning.debtamortization;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Pure, stateless calculation of a fixed-payment amortization schedule from explicit,
 * caller-supplied inputs. Callable independently of HTTP, persistence, and any LLM.
 *
 * <p>Model conventions:
 *
 * <ul>
 *   <li>{@code monthlyInterestRate} is an explicit monthly rate, not an annual rate; 0.01 means
 *       1% per month. It is never inferred or converted.
 *   <li>Each month, interest accrues on the opening balance, then the payment is applied at
 *       month end: {@code payment = min(monthlyPayment, openingBalance + interest)}.
 *   <li>Interest is rounded to 2 decimal places with HALF_UP before the payment is applied; all
 *       monetary values are kept at 2 decimals.
 *   <li>The schedule is bounded to {@value #MAX_HORIZON_MONTHS} months.
 * </ul>
 *
 * <p>This is an illustrative constant-rate schedule, not a lender payoff quote.
 */
public final class DebtAmortizationCalculator {

    static final int MAX_HORIZON_MONTHS = 1200;

    private static final int MONEY_SCALE = 2;
    private static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_UP;
    private static final int MAX_MONEY_INTEGER_DIGITS = 17;
    private static final int MAX_RATE_FRACTION_DIGITS = 8;
    private static final BigDecimal ZERO_MONEY = BigDecimal.ZERO.setScale(MONEY_SCALE, MONEY_ROUNDING);

    private DebtAmortizationCalculator() {
    }

    public static DebtAmortizationResult calculate(
            BigDecimal principal,
            BigDecimal monthlyInterestRate,
            BigDecimal monthlyPayment,
            String currency
    ) {
        validate(principal, monthlyInterestRate, monthlyPayment, currency);
        BigDecimal normalizedPrincipal = principal.setScale(MONEY_SCALE, MONEY_ROUNDING);
        BigDecimal normalizedPayment = monthlyPayment.setScale(MONEY_SCALE, MONEY_ROUNDING);
        String normalizedCurrency = currency.trim().toUpperCase(Locale.ROOT);

        if (normalizedPrincipal.compareTo(ZERO_MONEY) == 0) {
            return new DebtAmortizationResult(
                    normalizedPrincipal, monthlyInterestRate, normalizedPayment, normalizedCurrency,
                    DebtAmortizationStatus.PAID_OFF, 0, ZERO_MONEY, ZERO_MONEY, ZERO_MONEY, List.of());
        }

        BigDecimal firstInterest = roundMoney(normalizedPrincipal.multiply(monthlyInterestRate));
        if (normalizedPayment.compareTo(firstInterest) <= 0) {
            return new DebtAmortizationResult(
                    normalizedPrincipal, monthlyInterestRate, normalizedPayment, normalizedCurrency,
                    DebtAmortizationStatus.NON_AMORTIZING, null, ZERO_MONEY, ZERO_MONEY, normalizedPrincipal,
                    List.of());
        }

        List<DebtAmortizationRow> rows = new ArrayList<>();
        BigDecimal opening = normalizedPrincipal;
        BigDecimal totalPaid = ZERO_MONEY;
        BigDecimal totalInterest = ZERO_MONEY;
        Integer payoffMonth = null;

        for (int month = 1; month <= MAX_HORIZON_MONTHS; month++) {
            BigDecimal interest = roundMoney(opening.multiply(monthlyInterestRate));
            BigDecimal amountOwed = opening.add(interest);
            BigDecimal payment = normalizedPayment.compareTo(amountOwed) < 0 ? normalizedPayment : amountOwed;
            BigDecimal closing = amountOwed.subtract(payment);
            BigDecimal principalRepaid = payment.subtract(interest);

            rows.add(new DebtAmortizationRow(month, opening, interest, payment, principalRepaid, closing));
            totalPaid = totalPaid.add(payment);
            totalInterest = totalInterest.add(interest);
            opening = closing;

            if (closing.compareTo(ZERO_MONEY) == 0) {
                payoffMonth = month;
                break;
            }
        }

        DebtAmortizationStatus status = payoffMonth != null
                ? DebtAmortizationStatus.PAID_OFF
                : DebtAmortizationStatus.HORIZON_LIMIT;
        BigDecimal remainingBalance = status == DebtAmortizationStatus.PAID_OFF ? ZERO_MONEY : opening;

        return new DebtAmortizationResult(
                normalizedPrincipal, monthlyInterestRate, normalizedPayment, normalizedCurrency,
                status, payoffMonth, totalPaid, totalInterest, remainingBalance, List.copyOf(rows));
    }

    private static BigDecimal roundMoney(BigDecimal value) {
        return value.setScale(MONEY_SCALE, MONEY_ROUNDING);
    }

    private static void validate(
            BigDecimal principal, BigDecimal monthlyInterestRate, BigDecimal monthlyPayment, String currency
    ) {
        if (principal == null) {
            throw new InvalidDebtAmortizationInputException("principal must not be null");
        }
        if (monthlyInterestRate == null) {
            throw new InvalidDebtAmortizationInputException("monthlyInterestRate must not be null");
        }
        if (monthlyPayment == null) {
            throw new InvalidDebtAmortizationInputException("monthlyPayment must not be null");
        }
        if (currency == null || currency.isBlank()) {
            throw new InvalidDebtAmortizationInputException("currency must not be blank");
        }
        if (principal.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidDebtAmortizationInputException("principal must not be negative");
        }
        if (monthlyPayment.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidDebtAmortizationInputException("monthlyPayment must be positive");
        }
        if (monthlyInterestRate.compareTo(BigDecimal.ZERO) < 0 || monthlyInterestRate.compareTo(BigDecimal.ONE) > 0) {
            throw new InvalidDebtAmortizationInputException("monthlyInterestRate must be between 0 and 1 inclusive");
        }
        if (monthlyInterestRate.scale() > MAX_RATE_FRACTION_DIGITS) {
            throw new InvalidDebtAmortizationInputException(
                    "monthlyInterestRate must have at most " + MAX_RATE_FRACTION_DIGITS + " fractional digits");
        }
        validateMoneyDigits(principal, "principal");
        validateMoneyDigits(monthlyPayment, "monthlyPayment");
        if (!currency.trim().matches("^[A-Za-z]{3}$")) {
            throw new InvalidDebtAmortizationInputException("currency must be a 3-letter currency code");
        }
    }

    private static void validateMoneyDigits(BigDecimal value, String field) {
        if (value.scale() > MONEY_SCALE) {
            throw new InvalidDebtAmortizationInputException(
                    field + " must have at most " + MONEY_SCALE + " fractional digits");
        }
        int integerDigits = value.precision() - value.scale();
        if (integerDigits > MAX_MONEY_INTEGER_DIGITS) {
            throw new InvalidDebtAmortizationInputException(
                    field + " must have at most " + MAX_MONEY_INTEGER_DIGITS + " integer digits");
        }
    }
}
