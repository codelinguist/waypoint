package com.waypoint.planning.cashflow;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * Projects a dated, month-by-month cash balance from an explicit starting cash balance and
 * constant monthly inflow/outflow assumptions supplied by the caller.
 *
 * <p>This is a disposable, stateless calculation over caller-supplied temporary inputs: it
 * assumes the same inflow and outflow recur unchanged every month, ignores timing within a
 * month and any irregular event, and does not read or write any persisted household state. It
 * never establishes an actual household cash-flow forecast, recommendation, or approved
 * decision. It enforces its own input invariants so it rejects invalid values whether it is
 * called directly or through the HTTP layer, which applies the same rules independently via
 * request validation.
 *
 * <p>Every monetary amount is kept at exactly 2 decimal places. Since a month's balance is
 * derived only by addition and subtraction of already-2-decimal values, no rounding is ever
 * applied to a derived balance; {@link BigDecimal} arithmetic also never overflows the way a
 * primitive numeric type could across up to {@value #MAX_HORIZON_MONTHS} months.
 */
@Service
public class CashFlowProjectionCalculator {

    static final int MAX_HORIZON_MONTHS = 1200;
    static final int MIN_HORIZON_MONTHS = 1;

    private static final int MONEY_SCALE = 2;
    private static final int MAX_MONEY_INTEGER_DIGITS = 17;
    private static final Pattern CURRENCY_CODE = Pattern.compile("^[A-Za-z]{3}$");

    public CashFlowProjectionResult calculate(
            String currency,
            YearMonth startMonth,
            BigDecimal startingCash,
            BigDecimal monthlyInflow,
            BigDecimal monthlyOutflow,
            int months
    ) {
        String normalizedCurrency = validateCurrency(currency);
        if (startMonth == null) {
            throw new InvalidCashFlowProjectionInputException("startMonth must not be null");
        }
        BigDecimal normalizedStartingCash = validateMoney(startingCash, "startingCash");
        BigDecimal normalizedInflow = validateMoney(monthlyInflow, "monthlyInflow");
        BigDecimal normalizedOutflow = validateMoney(monthlyOutflow, "monthlyOutflow");
        validateMonths(months);

        BigDecimal netCashFlow = normalizedInflow.subtract(normalizedOutflow);

        List<CashFlowProjectionRow> rows = new ArrayList<>(months);
        BigDecimal opening = normalizedStartingCash;
        BigDecimal lowestClosingBalance = null;
        YearMonth lowestClosingBalanceMonth = null;
        YearMonth firstNegativeMonth = null;

        for (int index = 0; index < months; index++) {
            YearMonth month = startMonth.plusMonths(index);
            BigDecimal closing = opening.add(netCashFlow);

            rows.add(new CashFlowProjectionRow(month, opening, normalizedInflow, normalizedOutflow, netCashFlow, closing));

            if (lowestClosingBalance == null || closing.compareTo(lowestClosingBalance) < 0) {
                lowestClosingBalance = closing;
                lowestClosingBalanceMonth = month;
            }
            if (firstNegativeMonth == null && closing.signum() < 0) {
                firstNegativeMonth = month;
            }

            opening = closing;
        }

        BigDecimal endingCash = opening;
        CashFlowProjectionStatus status = firstNegativeMonth != null
                ? CashFlowProjectionStatus.BECOMES_NEGATIVE
                : CashFlowProjectionStatus.REMAINS_NONNEGATIVE;

        return new CashFlowProjectionResult(
                normalizedCurrency,
                startMonth,
                normalizedStartingCash,
                normalizedInflow,
                normalizedOutflow,
                months,
                List.copyOf(rows),
                endingCash,
                lowestClosingBalance,
                lowestClosingBalanceMonth,
                firstNegativeMonth,
                status
        );
    }

    private String validateCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            throw new InvalidCashFlowProjectionInputException("currency must not be blank");
        }
        if (!CURRENCY_CODE.matcher(currency).matches()) {
            throw new InvalidCashFlowProjectionInputException("currency must be a 3-letter currency code");
        }
        return currency.toUpperCase(Locale.ROOT);
    }

    private BigDecimal validateMoney(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new InvalidCashFlowProjectionInputException(fieldName + " must not be null");
        }
        if (value.scale() > MONEY_SCALE) {
            throw new InvalidCashFlowProjectionInputException(
                    fieldName + " must have at most " + MONEY_SCALE + " fraction digits");
        }
        int integerDigits = Math.max(value.precision() - value.scale(), 0);
        if (integerDigits > MAX_MONEY_INTEGER_DIGITS) {
            throw new InvalidCashFlowProjectionInputException(
                    fieldName + " must have at most " + MAX_MONEY_INTEGER_DIGITS + " integer digits");
        }
        if (value.signum() < 0) {
            throw new InvalidCashFlowProjectionInputException(fieldName + " must not be negative");
        }
        return value.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
    }

    private void validateMonths(int months) {
        if (months < MIN_HORIZON_MONTHS || months > MAX_HORIZON_MONTHS) {
            throw new InvalidCashFlowProjectionInputException(
                    "months must be between " + MIN_HORIZON_MONTHS + " and " + MAX_HORIZON_MONTHS);
        }
    }
}
