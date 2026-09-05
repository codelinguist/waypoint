package com.waypoint.planning.runway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;

class EmergencyFundRunwayCalculatorTest {

    private final EmergencyFundRunwayCalculator calculator = new EmergencyFundRunwayCalculator();

    @Test
    void computesFiniteRunwayForAPositiveShortfall() {
        EmergencyFundRunwayResult result = calculator.calculate(
                new BigDecimal("1000.00"), new BigDecimal("400.00"), new BigDecimal("100.00"), "USD");

        assertThat(result.status()).isEqualTo(RunwayStatus.FINITE);
        assertThat(result.monthlyShortfall()).isEqualByComparingTo("300.00");
        assertThat(result.runwayMonths()).isEqualByComparingTo("3.33");
        assertThat(result.fullMonthsCovered()).isEqualTo(BigInteger.valueOf(3));
        assertThat(result.currency()).isEqualTo("USD");
    }

    @Test
    void roundsRunwayMonthsDownRatherThanToTheNearestHundredth() {
        // 1000 / 600 = 1.6666..., which rounds to 1.67 at the nearest hundredth
        // but must be truncated down to 1.66 per the product contract.
        EmergencyFundRunwayResult result = calculator.calculate(
                new BigDecimal("1000.00"), new BigDecimal("700.00"), new BigDecimal("100.00"), "USD");

        assertThat(result.monthlyShortfall()).isEqualByComparingTo("600.00");
        assertThat(result.runwayMonths()).isEqualByComparingTo("1.66");
        assertThat(result.fullMonthsCovered()).isEqualTo(BigInteger.valueOf(1));
    }

    @Test
    void derivesFullMonthsCoveredFromTheUnroundedRatioNotFromRunwayMonths() {
        // 999 / 333 = 3 exactly, and the rounded-down two-decimal runway is
        // also exactly 3.00 here, so this exercises the boundary where a
        // naive floor-of-the-rounded-value implementation would still agree;
        // combined with the 1.6666.. case above, together they confirm
        // fullMonthsCovered is not derived by flooring runwayMonths.
        EmergencyFundRunwayResult result = calculator.calculate(
                new BigDecimal("999.00"), new BigDecimal("333.00"), BigDecimal.ZERO, "USD");

        assertThat(result.runwayMonths()).isEqualByComparingTo("3.00");
        assertThat(result.fullMonthsCovered()).isEqualTo(BigInteger.valueOf(3));
    }

    @Test
    void returnsZeroRunwayAndZeroFullMonthsForAZeroReserveWithAPositiveShortfall() {
        EmergencyFundRunwayResult result = calculator.calculate(
                BigDecimal.ZERO, new BigDecimal("400.00"), new BigDecimal("100.00"), "USD");

        assertThat(result.status()).isEqualTo(RunwayStatus.FINITE);
        assertThat(result.runwayMonths()).isEqualByComparingTo("0.00");
        assertThat(result.fullMonthsCovered()).isEqualTo(BigInteger.ZERO);
    }

    @Test
    void returnsNoShortfallWithNullMonthValuesWhenIncomeEqualsExpenses() {
        EmergencyFundRunwayResult result = calculator.calculate(
                new BigDecimal("1000.00"), new BigDecimal("400.00"), new BigDecimal("400.00"), "USD");

        assertThat(result.status()).isEqualTo(RunwayStatus.NO_SHORTFALL);
        assertThat(result.monthlyShortfall()).isEqualByComparingTo("0.00");
        assertThat(result.runwayMonths()).isNull();
        assertThat(result.fullMonthsCovered()).isNull();
    }

    @Test
    void returnsNoShortfallWhenIncomeExceedsExpenses() {
        EmergencyFundRunwayResult result = calculator.calculate(
                new BigDecimal("1000.00"), new BigDecimal("400.00"), new BigDecimal("500.00"), "USD");

        assertThat(result.status()).isEqualTo(RunwayStatus.NO_SHORTFALL);
        assertThat(result.runwayMonths()).isNull();
        assertThat(result.fullMonthsCovered()).isNull();
    }

    @Test
    void returnsNoShortfallWhenEveryInputIsZero() {
        EmergencyFundRunwayResult result = calculator.calculate(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "USD");

        assertThat(result.status()).isEqualTo(RunwayStatus.NO_SHORTFALL);
        assertThat(result.monthlyShortfall()).isEqualByComparingTo("0.00");
        assertThat(result.runwayMonths()).isNull();
        assertThat(result.fullMonthsCovered()).isNull();
    }

    @Test
    void normalizesCurrencyCaseAndTrimsWhitespace() {
        EmergencyFundRunwayResult result = calculator.calculate(
                new BigDecimal("1000.00"), new BigDecimal("400.00"), new BigDecimal("100.00"), " usd ");

        assertThat(result.currency()).isEqualTo("USD");
    }

    @Test
    void identicalInputsProduceIdenticalResults() {
        EmergencyFundRunwayResult first = calculator.calculate(
                new BigDecimal("1000.00"), new BigDecimal("400.00"), new BigDecimal("100.00"), "USD");
        EmergencyFundRunwayResult second = calculator.calculate(
                new BigDecimal("1000.00"), new BigDecimal("400.00"), new BigDecimal("100.00"), "USD");

        assertThat(first).isEqualTo(second);
    }

    @Test
    void rejectsNullAvailableReserve() {
        assertThatThrownBy(() -> calculator.calculate(
                null, new BigDecimal("400.00"), new BigDecimal("100.00"), "USD"))
                .isInstanceOf(InvalidRunwayInputException.class)
                .hasMessageContaining("availableReserve");
    }

    @Test
    void rejectsNullMonthlyExpenses() {
        assertThatThrownBy(() -> calculator.calculate(
                new BigDecimal("1000.00"), null, new BigDecimal("100.00"), "USD"))
                .isInstanceOf(InvalidRunwayInputException.class)
                .hasMessageContaining("monthlyExpenses");
    }

    @Test
    void rejectsNullMonthlyNetIncome() {
        assertThatThrownBy(() -> calculator.calculate(
                new BigDecimal("1000.00"), new BigDecimal("400.00"), null, "USD"))
                .isInstanceOf(InvalidRunwayInputException.class)
                .hasMessageContaining("monthlyNetIncome");
    }

    @Test
    void rejectsNegativeAvailableReserve() {
        assertThatThrownBy(() -> calculator.calculate(
                new BigDecimal("-0.01"), new BigDecimal("400.00"), new BigDecimal("100.00"), "USD"))
                .isInstanceOf(InvalidRunwayInputException.class)
                .hasMessageContaining("must not be negative");
    }

    @Test
    void rejectsNegativeMonthlyExpenses() {
        assertThatThrownBy(() -> calculator.calculate(
                new BigDecimal("1000.00"), new BigDecimal("-1.00"), new BigDecimal("100.00"), "USD"))
                .isInstanceOf(InvalidRunwayInputException.class)
                .hasMessageContaining("must not be negative");
    }

    @Test
    void rejectsNegativeMonthlyNetIncome() {
        assertThatThrownBy(() -> calculator.calculate(
                new BigDecimal("1000.00"), new BigDecimal("400.00"), new BigDecimal("-1.00"), "USD"))
                .isInstanceOf(InvalidRunwayInputException.class)
                .hasMessageContaining("must not be negative");
    }

    @Test
    void rejectsExcessiveFractionDigits() {
        assertThatThrownBy(() -> calculator.calculate(
                new BigDecimal("1000.123"), new BigDecimal("400.00"), new BigDecimal("100.00"), "USD"))
                .isInstanceOf(InvalidRunwayInputException.class)
                .hasMessageContaining("fraction digits");
    }

    @Test
    void rejectsExcessiveIntegerDigits() {
        BigDecimal tooLarge = new BigDecimal("100000000000000000.00");
        assertThatThrownBy(() -> calculator.calculate(
                tooLarge, new BigDecimal("400.00"), new BigDecimal("100.00"), "USD"))
                .isInstanceOf(InvalidRunwayInputException.class)
                .hasMessageContaining("integer digits");
    }

    @Test
    void rejectsNullCurrency() {
        assertThatThrownBy(() -> calculator.calculate(
                new BigDecimal("1000.00"), new BigDecimal("400.00"), new BigDecimal("100.00"), null))
                .isInstanceOf(InvalidRunwayInputException.class)
                .hasMessageContaining("currency");
    }

    @Test
    void rejectsBlankCurrency() {
        assertThatThrownBy(() -> calculator.calculate(
                new BigDecimal("1000.00"), new BigDecimal("400.00"), new BigDecimal("100.00"), "   "))
                .isInstanceOf(InvalidRunwayInputException.class)
                .hasMessageContaining("currency");
    }

    @Test
    void rejectsMalformedCurrencyCode() {
        assertThatThrownBy(() -> calculator.calculate(
                new BigDecimal("1000.00"), new BigDecimal("400.00"), new BigDecimal("100.00"), "US1"))
                .isInstanceOf(InvalidRunwayInputException.class)
                .hasMessageContaining("3-letter");
    }
}
