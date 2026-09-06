package com.waypoint.planning.cashflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CashFlowProjectionCalculatorTest {

    private final CashFlowProjectionCalculator calculator = new CashFlowProjectionCalculator();
    private Locale originalDefaultLocale;

    @BeforeEach
    void captureDefaultLocale() {
        originalDefaultLocale = Locale.getDefault();
    }

    @AfterEach
    void restoreDefaultLocale() {
        Locale.setDefault(originalDefaultLocale);
    }

    @Test
    void projectsAConstantOutflowExceedingInflowUntilTheBalanceBecomesNegative() {
        CashFlowProjectionResult result = calculator.calculate(
                "USD", YearMonth.of(2027, 1), new BigDecimal("1000.00"),
                new BigDecimal("300.00"), new BigDecimal("500.00"), 6);

        assertThat(result.rows()).hasSize(6);
        assertThat(result.rows().stream().map(CashFlowProjectionRow::closingCash))
                .containsExactly(
                        new BigDecimal("800.00"), new BigDecimal("600.00"), new BigDecimal("400.00"),
                        new BigDecimal("200.00"), new BigDecimal("0.00"), new BigDecimal("-200.00"));
        assertThat(result.status()).isEqualTo(CashFlowProjectionStatus.BECOMES_NEGATIVE);
        assertThat(result.firstNegativeMonth()).isEqualTo(YearMonth.of(2027, 6));
        assertThat(result.lowestClosingBalance()).isEqualByComparingTo("-200.00");
        assertThat(result.lowestClosingBalanceMonth()).isEqualTo(YearMonth.of(2027, 6));
        assertThat(result.endingCash()).isEqualByComparingTo("-200.00");
    }

    @Test
    void equalInflowAndOutflowPreservesTheStartingBalanceEveryMonth() {
        CashFlowProjectionResult result = calculator.calculate(
                "USD", YearMonth.of(2027, 1), new BigDecimal("500.00"),
                new BigDecimal("200.00"), new BigDecimal("200.00"), 4);

        assertThat(result.rows().stream().map(CashFlowProjectionRow::closingCash))
                .allMatch(closing -> closing.compareTo(new BigDecimal("500.00")) == 0);
        assertThat(result.status()).isEqualTo(CashFlowProjectionStatus.REMAINS_NONNEGATIVE);
        assertThat(result.firstNegativeMonth()).isNull();
        assertThat(result.lowestClosingBalance()).isEqualByComparingTo("500.00");
        assertThat(result.lowestClosingBalanceMonth()).isEqualTo(YearMonth.of(2027, 1));
    }

    @Test
    void positiveNetFlowIncreasesTheBalanceEachMonth() {
        CashFlowProjectionResult result = calculator.calculate(
                "USD", YearMonth.of(2027, 1), new BigDecimal("100.00"),
                new BigDecimal("300.00"), new BigDecimal("100.00"), 3);

        assertThat(result.rows().stream().map(CashFlowProjectionRow::closingCash))
                .containsExactly(new BigDecimal("300.00"), new BigDecimal("500.00"), new BigDecimal("700.00"));
        assertThat(result.status()).isEqualTo(CashFlowProjectionStatus.REMAINS_NONNEGATIVE);
        assertThat(result.lowestClosingBalance()).isEqualByComparingTo("300.00");
        assertThat(result.lowestClosingBalanceMonth()).isEqualTo(YearMonth.of(2027, 1));
    }

    @Test
    void zeroStartingCashWithZeroFlowsRemainsNonnegative() {
        CashFlowProjectionResult result = calculator.calculate(
                "USD", YearMonth.of(2027, 1), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 2);

        assertThat(result.rows().stream().map(CashFlowProjectionRow::closingCash))
                .containsExactly(new BigDecimal("0.00"), new BigDecimal("0.00"));
        assertThat(result.status()).isEqualTo(CashFlowProjectionStatus.REMAINS_NONNEGATIVE);
        assertThat(result.firstNegativeMonth()).isNull();
    }

    @Test
    void eachRowReconcilesAndTheNextOpeningEqualsThePreviousClosing() {
        CashFlowProjectionResult result = calculator.calculate(
                "USD", YearMonth.of(2027, 1), new BigDecimal("1000.00"),
                new BigDecimal("300.00"), new BigDecimal("500.00"), 6);

        for (CashFlowProjectionRow row : result.rows()) {
            assertThat(row.openingCash().add(row.inflow()).subtract(row.outflow()))
                    .isEqualByComparingTo(row.closingCash());
            assertThat(row.netCashFlow()).isEqualByComparingTo(row.inflow().subtract(row.outflow()));
        }
        for (int i = 1; i < result.rows().size(); i++) {
            assertThat(result.rows().get(i).openingCash())
                    .isEqualByComparingTo(result.rows().get(i - 1).closingCash());
        }
    }

    @Test
    void monthLabelsAdvanceCorrectlyAcrossAYearBoundary() {
        CashFlowProjectionResult result = calculator.calculate(
                "USD", YearMonth.of(2026, 11), new BigDecimal("100.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, 4);

        assertThat(result.rows().stream().map(CashFlowProjectionRow::month))
                .containsExactly(
                        YearMonth.of(2026, 11), YearMonth.of(2026, 12),
                        YearMonth.of(2027, 1), YearMonth.of(2027, 2));
    }

    @Test
    void firstOccurrenceOfATiedLowestBalanceIsReturned() {
        CashFlowProjectionResult result = calculator.calculate(
                "USD", YearMonth.of(2027, 1), new BigDecimal("100.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, 5);

        assertThat(result.lowestClosingBalance()).isEqualByComparingTo("100.00");
        assertThat(result.lowestClosingBalanceMonth()).isEqualTo(YearMonth.of(2027, 1));
    }

    @Test
    void zeroClosingBalanceIsNotTreatedAsFirstNegative() {
        CashFlowProjectionResult result = calculator.calculate(
                "USD", YearMonth.of(2027, 1), new BigDecimal("200.00"),
                BigDecimal.ZERO, new BigDecimal("100.00"), 2);

        assertThat(result.rows().get(1).closingCash()).isEqualByComparingTo("0.00");
        assertThat(result.firstNegativeMonth()).isNull();
        assertThat(result.status()).isEqualTo(CashFlowProjectionStatus.REMAINS_NONNEGATIVE);
    }

    @Test
    void onlyTheFirstNegativeMonthIsReported() {
        CashFlowProjectionResult result = calculator.calculate(
                "USD", YearMonth.of(2027, 1), new BigDecimal("100.00"),
                BigDecimal.ZERO, new BigDecimal("100.00"), 4);

        assertThat(result.firstNegativeMonth()).isEqualTo(YearMonth.of(2027, 2));
    }

    @Test
    void normalizesCurrencyToUppercase() {
        CashFlowProjectionResult result = calculator.calculate(
                "usd", YearMonth.of(2027, 1), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 1);

        assertThat(result.currency()).isEqualTo("USD");
    }

    @Test
    void normalizesCurrencyToUppercaseIndependentlyOfDefaultLocale() {
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));

        CashFlowProjectionResult result = calculator.calculate(
                "inr", YearMonth.of(2027, 1), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 1);

        assertThat(result.currency()).isEqualTo("INR");
    }

    @Test
    void doesNotTruncateADerivedBalanceEvenWhenInputsAreWholeNumbers() {
        CashFlowProjectionResult result = calculator.calculate(
                "USD", YearMonth.of(2027, 1), new BigDecimal("1000"),
                new BigDecimal("1"), new BigDecimal("2"), 1);

        assertThat(result.rows().get(0).closingCash()).isEqualByComparingTo("999.00");
        assertThat(result.rows().get(0).closingCash().scale()).isEqualTo(2);
    }

    @Test
    void rejectsNullCurrency() {
        assertThatThrownBy(() -> calculator.calculate(
                null, YearMonth.of(2027, 1), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 1))
                .isInstanceOf(InvalidCashFlowProjectionInputException.class);
    }

    @Test
    void rejectsMalformedCurrencyCode() {
        assertThatThrownBy(() -> calculator.calculate(
                "US", YearMonth.of(2027, 1), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 1))
                .isInstanceOf(InvalidCashFlowProjectionInputException.class);
    }

    @Test
    void rejectsNullStartMonth() {
        assertThatThrownBy(() -> calculator.calculate(
                "USD", null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 1))
                .isInstanceOf(InvalidCashFlowProjectionInputException.class);
    }

    @Test
    void rejectsNullStartingCash() {
        assertThatThrownBy(() -> calculator.calculate(
                "USD", YearMonth.of(2027, 1), null, BigDecimal.ZERO, BigDecimal.ZERO, 1))
                .isInstanceOf(InvalidCashFlowProjectionInputException.class);
    }

    @Test
    void rejectsNegativeStartingCash() {
        assertThatThrownBy(() -> calculator.calculate(
                "USD", YearMonth.of(2027, 1), new BigDecimal("-0.01"), BigDecimal.ZERO, BigDecimal.ZERO, 1))
                .isInstanceOf(InvalidCashFlowProjectionInputException.class);
    }

    @Test
    void rejectsNegativeMonthlyInflow() {
        assertThatThrownBy(() -> calculator.calculate(
                "USD", YearMonth.of(2027, 1), BigDecimal.ZERO, new BigDecimal("-0.01"), BigDecimal.ZERO, 1))
                .isInstanceOf(InvalidCashFlowProjectionInputException.class);
    }

    @Test
    void rejectsNegativeMonthlyOutflow() {
        assertThatThrownBy(() -> calculator.calculate(
                "USD", YearMonth.of(2027, 1), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("-0.01"), 1))
                .isInstanceOf(InvalidCashFlowProjectionInputException.class);
    }

    @Test
    void rejectsExcessiveFractionDigits() {
        assertThatThrownBy(() -> calculator.calculate(
                "USD", YearMonth.of(2027, 1), new BigDecimal("100.001"), BigDecimal.ZERO, BigDecimal.ZERO, 1))
                .isInstanceOf(InvalidCashFlowProjectionInputException.class);
    }

    @Test
    void rejectsExcessiveIntegerDigits() {
        BigDecimal tooLarge = new BigDecimal("100000000000000000.00");
        assertThatThrownBy(() -> calculator.calculate(
                "USD", YearMonth.of(2027, 1), tooLarge, BigDecimal.ZERO, BigDecimal.ZERO, 1))
                .isInstanceOf(InvalidCashFlowProjectionInputException.class);
    }

    @Test
    void rejectsIntegerDigitLimitBypassedByNegativeScaleRepresentation() {
        BigDecimal negativeScaleTooLarge = new BigDecimal("1E+17");
        assertThatThrownBy(() -> calculator.calculate(
                "USD", YearMonth.of(2027, 1), negativeScaleTooLarge, BigDecimal.ZERO, BigDecimal.ZERO, 1))
                .isInstanceOf(InvalidCashFlowProjectionInputException.class);
    }

    @Test
    void acceptsIntegerDigitLimitAtTheNegativeScaleBoundary() {
        CashFlowProjectionResult result = calculator.calculate(
                "USD", YearMonth.of(2027, 1), new BigDecimal("1E+16"), BigDecimal.ZERO, BigDecimal.ZERO, 1);

        assertThat(result.startingCash()).isEqualByComparingTo("1E+16");
    }

    @Test
    void rejectsZeroMonths() {
        assertThatThrownBy(() -> calculator.calculate(
                "USD", YearMonth.of(2027, 1), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0))
                .isInstanceOf(InvalidCashFlowProjectionInputException.class);
    }

    @Test
    void rejectsNegativeMonths() {
        assertThatThrownBy(() -> calculator.calculate(
                "USD", YearMonth.of(2027, 1), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, -1))
                .isInstanceOf(InvalidCashFlowProjectionInputException.class);
    }

    @Test
    void rejectsMonthsAboveTwelveHundred() {
        assertThatThrownBy(() -> calculator.calculate(
                "USD", YearMonth.of(2027, 1), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 1201))
                .isInstanceOf(InvalidCashFlowProjectionInputException.class);
    }

    @Test
    void acceptsMonthsAtTheLowerAndUpperBounds() {
        CashFlowProjectionResult lower = calculator.calculate(
                "USD", YearMonth.of(2027, 1), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 1);
        assertThat(lower.rows()).hasSize(1);

        CashFlowProjectionResult upper = calculator.calculate(
                "USD", YearMonth.of(2027, 1), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 1200);
        assertThat(upper.rows()).hasSize(1200);
        assertThat(upper.rows().get(1199).month()).isEqualTo(YearMonth.of(2027, 1).plusMonths(1199));
    }

    @Test
    void identicalInputsProduceIdenticalResults() {
        CashFlowProjectionResult first = calculator.calculate(
                "USD", YearMonth.of(2027, 1), new BigDecimal("1000.00"),
                new BigDecimal("300.00"), new BigDecimal("500.00"), 6);
        CashFlowProjectionResult second = calculator.calculate(
                "USD", YearMonth.of(2027, 1), new BigDecimal("1000.00"),
                new BigDecimal("300.00"), new BigDecimal("500.00"), 6);

        assertThat(first).isEqualTo(second);
    }
}
