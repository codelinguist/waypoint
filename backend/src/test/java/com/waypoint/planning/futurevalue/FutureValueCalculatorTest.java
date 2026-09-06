package com.waypoint.planning.futurevalue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FutureValueCalculatorTest {

    private final FutureValueCalculator calculator = new FutureValueCalculator();
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
    void followsMonthlySequencingExactlyForTheWorkedExample() {
        FutureValueResult result = calculator.calculate(
                "USD", new BigDecimal("1000.00"), new BigDecimal("100.00"), new BigDecimal("12.00"), 2);

        assertThat(result.schedule()).hasSize(2);
        FutureValueRow month1 = result.schedule().get(0);
        assertThat(month1.month()).isEqualTo(1);
        assertThat(month1.openingBalance()).isEqualByComparingTo("1000.00");
        assertThat(month1.growth()).isEqualByComparingTo("10.00");
        assertThat(month1.contribution()).isEqualByComparingTo("100.00");
        assertThat(month1.closingBalance()).isEqualByComparingTo("1110.00");

        FutureValueRow month2 = result.schedule().get(1);
        assertThat(month2.openingBalance()).isEqualByComparingTo("1110.00");
        assertThat(month2.growth()).isEqualByComparingTo("11.10");
        assertThat(month2.closingBalance()).isEqualByComparingTo("1221.10");

        assertThat(result.endingValue()).isEqualByComparingTo("1221.10");
        assertThat(result.totalContributed()).isEqualByComparingTo("1200.00");
        assertThat(result.totalGrowth()).isEqualByComparingTo("21.10");
    }

    @Test
    void zeroRateEndingValueEqualsPrincipalPlusAllContributions() {
        FutureValueResult result = calculator.calculate(
                "USD", new BigDecimal("500.00"), new BigDecimal("50.00"), BigDecimal.ZERO, 6);

        assertThat(result.endingValue()).isEqualByComparingTo("800.00");
        assertThat(result.totalContributed()).isEqualByComparingTo("800.00");
        assertThat(result.totalGrowth()).isEqualByComparingTo("0.00");
        result.schedule().forEach(row -> assertThat(row.growth()).isEqualByComparingTo("0.00"));
    }

    @Test
    void allZeroMoneyInputsProduceAValidZeroSchedule() {
        FutureValueResult result = calculator.calculate(
                "USD", BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("5.00"), 3);

        assertThat(result.endingValue()).isEqualByComparingTo("0.00");
        assertThat(result.totalContributed()).isEqualByComparingTo("0.00");
        assertThat(result.totalGrowth()).isEqualByComparingTo("0.00");
        result.schedule().forEach(row -> {
            assertThat(row.openingBalance()).isEqualByComparingTo("0.00");
            assertThat(row.growth()).isEqualByComparingTo("0.00");
            assertThat(row.closingBalance()).isEqualByComparingTo("0.00");
        });
    }

    @Test
    void everyRowReconcilesOpeningPlusGrowthPlusContributionEqualsClosing() {
        FutureValueResult result = calculator.calculate(
                "USD", new BigDecimal("777.77"), new BigDecimal("23.45"), new BigDecimal("7.25"), 24);

        for (FutureValueRow row : result.schedule()) {
            assertThat(row.openingBalance().add(row.growth()).add(row.contribution()))
                    .isEqualByComparingTo(row.closingBalance());
        }
        FutureValueRow lastRow = result.schedule().get(result.schedule().size() - 1);
        assertThat(result.endingValue()).isEqualByComparingTo(lastRow.closingBalance());

        BigDecimal totalGrowth = result.schedule().stream()
                .map(FutureValueRow::growth)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(result.totalGrowth()).isEqualByComparingTo(totalGrowth);
        assertThat(result.endingValue()).isEqualByComparingTo(result.totalContributed().add(result.totalGrowth()));
    }

    @Test
    void normalizesCurrencyToUppercase() {
        FutureValueResult result = calculator.calculate(
                "usd", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 1);

        assertThat(result.currency()).isEqualTo("USD");
    }

    @Test
    void normalizesCurrencyToUppercaseIndependentlyOfDefaultLocale() {
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));

        FutureValueResult result = calculator.calculate(
                "inr", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 1);

        assertThat(result.currency()).isEqualTo("INR");
    }

    @Test
    void rejectsNullCurrency() {
        assertThatThrownBy(() -> calculator.calculate(
                null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 1))
                .isInstanceOf(InvalidFutureValueInputException.class);
    }

    @Test
    void rejectsMalformedCurrencyCode() {
        assertThatThrownBy(() -> calculator.calculate(
                "US", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 1))
                .isInstanceOf(InvalidFutureValueInputException.class);
        assertThatThrownBy(() -> calculator.calculate(
                "USD1", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 1))
                .isInstanceOf(InvalidFutureValueInputException.class);
    }

    @Test
    void rejectsNullStartingPrincipal() {
        assertThatThrownBy(() -> calculator.calculate(
                "USD", null, BigDecimal.ZERO, BigDecimal.ZERO, 1))
                .isInstanceOf(InvalidFutureValueInputException.class);
    }

    @Test
    void rejectsNegativeStartingPrincipal() {
        assertThatThrownBy(() -> calculator.calculate(
                "USD", new BigDecimal("-0.01"), BigDecimal.ZERO, BigDecimal.ZERO, 1))
                .isInstanceOf(InvalidFutureValueInputException.class);
    }

    @Test
    void rejectsNegativeMonthlyContribution() {
        assertThatThrownBy(() -> calculator.calculate(
                "USD", BigDecimal.ZERO, new BigDecimal("-0.01"), BigDecimal.ZERO, 1))
                .isInstanceOf(InvalidFutureValueInputException.class);
    }

    @Test
    void rejectsNegativeAnnualRate() {
        assertThatThrownBy(() -> calculator.calculate(
                "USD", BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("-0.01"), 1))
                .isInstanceOf(InvalidFutureValueInputException.class);
    }

    @Test
    void rejectsExcessiveFractionDigitsOnMoneyFields() {
        assertThatThrownBy(() -> calculator.calculate(
                "USD", new BigDecimal("100.001"), BigDecimal.ZERO, BigDecimal.ZERO, 1))
                .isInstanceOf(InvalidFutureValueInputException.class);
        assertThatThrownBy(() -> calculator.calculate(
                "USD", BigDecimal.ZERO, new BigDecimal("100.001"), BigDecimal.ZERO, 1))
                .isInstanceOf(InvalidFutureValueInputException.class);
    }

    @Test
    void rejectsExcessiveIntegerDigitsOnMoneyFields() {
        BigDecimal tooLarge = new BigDecimal("100000000000000000.00");
        assertThatThrownBy(() -> calculator.calculate("USD", tooLarge, BigDecimal.ZERO, BigDecimal.ZERO, 1))
                .isInstanceOf(InvalidFutureValueInputException.class);
    }

    @Test
    void rejectsIntegerDigitLimitBypassedByNegativeScaleRepresentation() {
        BigDecimal negativeScaleTooLarge = new BigDecimal("1E+17");
        assertThatThrownBy(() -> calculator.calculate("USD", negativeScaleTooLarge, BigDecimal.ZERO, BigDecimal.ZERO, 1))
                .isInstanceOf(InvalidFutureValueInputException.class);
    }

    @Test
    void acceptsIntegerDigitLimitAtTheNegativeScaleBoundary() {
        FutureValueResult result = calculator.calculate(
                "USD", new BigDecimal("1E+16"), BigDecimal.ZERO, BigDecimal.ZERO, 1);

        assertThat(result.startingPrincipal()).isEqualByComparingTo("1E+16");
    }

    @Test
    void rejectsExcessiveFractionDigitsOnAnnualRate() {
        assertThatThrownBy(() -> calculator.calculate(
                "USD", BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("1.00001"), 1))
                .isInstanceOf(InvalidFutureValueInputException.class);
    }

    @Test
    void rejectsExcessiveIntegerDigitsOnAnnualRate() {
        assertThatThrownBy(() -> calculator.calculate(
                "USD", BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("1000.00"), 1))
                .isInstanceOf(InvalidFutureValueInputException.class);
    }

    @Test
    void rejectsZeroProjectionMonths() {
        assertThatThrownBy(() -> calculator.calculate(
                "USD", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0))
                .isInstanceOf(InvalidFutureValueInputException.class);
    }

    @Test
    void rejectsNegativeProjectionMonths() {
        assertThatThrownBy(() -> calculator.calculate(
                "USD", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, -1))
                .isInstanceOf(InvalidFutureValueInputException.class);
    }

    @Test
    void rejectsProjectionMonthsAboveTwelveHundred() {
        assertThatThrownBy(() -> calculator.calculate(
                "USD", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 1201))
                .isInstanceOf(InvalidFutureValueInputException.class);
    }

    @Test
    void acceptsProjectionMonthsAtTheLowerAndUpperBounds() {
        assertThat(calculator.calculate("USD", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 1).schedule())
                .hasSize(1);
        assertThat(calculator.calculate("USD", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 1200).schedule())
                .hasSize(1200);
    }

    @Test
    void identicalInputsProduceIdenticalResults() {
        FutureValueResult first = calculator.calculate(
                "USD", new BigDecimal("1000.00"), new BigDecimal("100.00"), new BigDecimal("12.00"), 6);
        FutureValueResult second = calculator.calculate(
                "USD", new BigDecimal("1000.00"), new BigDecimal("100.00"), new BigDecimal("12.00"), 6);

        assertThat(first).isEqualTo(second);
    }

    @Test
    void conventionsStatementNamesTheAppliedConventions() {
        FutureValueResult result = calculator.calculate(
                "USD", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 1);

        assertThat(result.conventions())
                .contains("annual rate divided by 12")
                .contains("HALF_UP")
                .contains("not a guaranteed");
    }
}
