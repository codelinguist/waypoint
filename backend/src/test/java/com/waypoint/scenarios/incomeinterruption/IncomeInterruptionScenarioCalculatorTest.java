package com.waypoint.scenarios.incomeinterruption;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IncomeInterruptionScenarioCalculatorTest {

    private final IncomeInterruptionScenarioCalculator calculator = new IncomeInterruptionScenarioCalculator();
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
    void acceptanceExampleComputesBaselineScenarioAndExtraReserveNeeded() {
        IncomeInterruptionScenarioResult result = calculator.calculate(
                "USD", new BigDecimal("100.00"), new BigDecimal("100.00"), BigDecimal.ZERO,
                new BigDecimal("80.00"), 3, 2, 2);

        assertThat(result.baselineRows().stream().map(IncomeInterruptionScenarioRow::closingCash))
                .containsExactly(new BigDecimal("120.00"), new BigDecimal("140.00"), new BigDecimal("160.00"));
        assertThat(result.scenarioRows().stream().map(IncomeInterruptionScenarioRow::closingCash))
                .containsExactly(new BigDecimal("120.00"), new BigDecimal("40.00"), new BigDecimal("-40.00"));
        assertThat(result.additionalOpeningReserveNeeded()).isEqualByComparingTo("40.00");
        assertThat(result.firstNegativeMonth()).isEqualTo(3);
        assertThat(result.minimumCash()).isEqualByComparingTo("-40.00");
        assertThat(result.endingCash()).isEqualByComparingTo("-40.00");
    }

    @Test
    void noLossInterruptionYieldsIdenticalPathsAndZeroDeltas() {
        IncomeInterruptionScenarioResult result = calculator.calculate(
                "USD", new BigDecimal("100.00"), new BigDecimal("100.00"), new BigDecimal("100.00"),
                new BigDecimal("80.00"), 3, 2, 2);

        assertThat(result.scenarioRows().stream().map(IncomeInterruptionScenarioRow::closingCash))
                .containsExactlyElementsOf(
                        result.baselineRows().stream().map(IncomeInterruptionScenarioRow::closingCash).toList());
        assertThat(result.closingDeltas()).allMatch(delta -> delta.compareTo(BigDecimal.ZERO) == 0);
        assertThat(result.firstNegativeMonth()).isNull();
        assertThat(result.additionalOpeningReserveNeeded()).isEqualByComparingTo("0.00");
    }

    @Test
    void interruptionStartingAtMonthOneObeysIntervalBoundary() {
        IncomeInterruptionScenarioResult result = calculator.calculate(
                "USD", new BigDecimal("100.00"), new BigDecimal("100.00"), BigDecimal.ZERO,
                new BigDecimal("80.00"), 3, 1, 1);

        assertThat(result.scenarioRows().get(0).income()).isEqualByComparingTo("0.00");
        assertThat(result.scenarioRows().get(1).income()).isEqualByComparingTo("100.00");
        assertThat(result.scenarioRows().get(2).income()).isEqualByComparingTo("100.00");
    }

    @Test
    void interruptionEndingAtTheHorizonObeysIntervalBoundary() {
        IncomeInterruptionScenarioResult result = calculator.calculate(
                "USD", new BigDecimal("100.00"), new BigDecimal("100.00"), BigDecimal.ZERO,
                new BigDecimal("80.00"), 3, 2, 2);

        assertThat(result.scenarioRows().get(0).income()).isEqualByComparingTo("100.00");
        assertThat(result.scenarioRows().get(1).income()).isEqualByComparingTo("0.00");
        assertThat(result.scenarioRows().get(2).income()).isEqualByComparingTo("0.00");
    }

    @Test
    void zeroClosingCashIsNotNegative() {
        IncomeInterruptionScenarioResult result = calculator.calculate(
                "USD", new BigDecimal("80.00"), new BigDecimal("100.00"), BigDecimal.ZERO,
                new BigDecimal("80.00"), 2, 1, 1);

        assertThat(result.scenarioRows().get(0).closingCash()).isEqualByComparingTo("0.00");
        assertThat(result.firstNegativeMonth()).isNull();
    }

    @Test
    void everyRowReconcilesAndOpeningEqualsPreviousClosingWithinEachPath() {
        IncomeInterruptionScenarioResult result = calculator.calculate(
                "USD", new BigDecimal("100.00"), new BigDecimal("100.00"), BigDecimal.ZERO,
                new BigDecimal("80.00"), 4, 2, 2);

        for (IncomeInterruptionScenarioRow row : result.baselineRows()) {
            assertThat(row.openingCash().add(row.income()).subtract(row.expenses()))
                    .isEqualByComparingTo(row.closingCash());
        }
        for (IncomeInterruptionScenarioRow row : result.scenarioRows()) {
            assertThat(row.openingCash().add(row.income()).subtract(row.expenses()))
                    .isEqualByComparingTo(row.closingCash());
        }
        for (int i = 1; i < result.baselineRows().size(); i++) {
            assertThat(result.baselineRows().get(i).openingCash())
                    .isEqualByComparingTo(result.baselineRows().get(i - 1).closingCash());
            assertThat(result.scenarioRows().get(i).openingCash())
                    .isEqualByComparingTo(result.scenarioRows().get(i - 1).closingCash());
        }
    }

    @Test
    void recoversAfterTheInterruptionEndsAndIncomeResumes() {
        IncomeInterruptionScenarioResult result = calculator.calculate(
                "USD", new BigDecimal("100.00"), new BigDecimal("100.00"), BigDecimal.ZERO,
                new BigDecimal("80.00"), 5, 2, 2);

        assertThat(result.scenarioRows().stream().map(IncomeInterruptionScenarioRow::closingCash))
                .containsExactly(
                        new BigDecimal("120.00"), new BigDecimal("40.00"), new BigDecimal("-40.00"),
                        new BigDecimal("-20.00"), new BigDecimal("0.00"));
        assertThat(result.firstNegativeMonth()).isEqualTo(3);
    }

    @Test
    void minimumCashIncludesOpeningReserveWhenTheScenarioNeverDipsBelowIt() {
        IncomeInterruptionScenarioResult result = calculator.calculate(
                "USD", new BigDecimal("500.00"), new BigDecimal("100.00"), new BigDecimal("100.00"),
                new BigDecimal("50.00"), 3, 1, 1);

        assertThat(result.minimumCash()).isEqualByComparingTo("500.00");
        assertThat(result.additionalOpeningReserveNeeded()).isEqualByComparingTo("0.00");
    }

    @Test
    void firstOccurrenceOfTheFirstNegativeMonthIsReported() {
        IncomeInterruptionScenarioResult result = calculator.calculate(
                "USD", new BigDecimal("50.00"), new BigDecimal("100.00"), BigDecimal.ZERO,
                new BigDecimal("80.00"), 4, 1, 3);

        assertThat(result.firstNegativeMonth()).isEqualTo(1);
    }

    @Test
    void normalizesCurrencyToUppercaseIndependentlyOfDefaultLocale() {
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));

        IncomeInterruptionScenarioResult result = calculator.calculate(
                "inr", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 1, 1, 1);

        assertThat(result.currency()).isEqualTo("INR");
    }

    @Test
    void doesNotTruncateADerivedBalanceEvenWhenInputsAreWholeNumbers() {
        IncomeInterruptionScenarioResult result = calculator.calculate(
                "USD", new BigDecimal("1000"), new BigDecimal("1"), BigDecimal.ZERO,
                new BigDecimal("2"), 1, 1, 1);

        assertThat(result.scenarioRows().get(0).closingCash()).isEqualByComparingTo("998.00");
        assertThat(result.scenarioRows().get(0).closingCash().scale()).isEqualTo(2);
    }

    @Test
    void identicalInputsProduceIdenticalResults() {
        IncomeInterruptionScenarioResult first = calculator.calculate(
                "USD", new BigDecimal("100.00"), new BigDecimal("100.00"), BigDecimal.ZERO,
                new BigDecimal("80.00"), 3, 2, 2);
        IncomeInterruptionScenarioResult second = calculator.calculate(
                "USD", new BigDecimal("100.00"), new BigDecimal("100.00"), BigDecimal.ZERO,
                new BigDecimal("80.00"), 3, 2, 2);

        assertThat(first).isEqualTo(second);
    }

    @Test
    void rejectsNullCurrency() {
        assertThatThrownBy(() -> calculator.calculate(
                null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 1, 1, 1))
                .isInstanceOf(InvalidIncomeInterruptionScenarioInputException.class);
    }

    @Test
    void rejectsMalformedCurrencyCode() {
        assertThatThrownBy(() -> calculator.calculate(
                "US", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 1, 1, 1))
                .isInstanceOf(InvalidIncomeInterruptionScenarioInputException.class);
    }

    @Test
    void rejectsNullOpeningReserve() {
        assertThatThrownBy(() -> calculator.calculate(
                "USD", null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 1, 1, 1))
                .isInstanceOf(InvalidIncomeInterruptionScenarioInputException.class);
    }

    @Test
    void rejectsNegativeOpeningReserve() {
        assertThatThrownBy(() -> calculator.calculate(
                "USD", new BigDecimal("-0.01"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 1, 1, 1))
                .isInstanceOf(InvalidIncomeInterruptionScenarioInputException.class);
    }

    @Test
    void rejectsNegativeNormalMonthlyNetIncome() {
        assertThatThrownBy(() -> calculator.calculate(
                "USD", BigDecimal.ZERO, new BigDecimal("-0.01"), BigDecimal.ZERO, BigDecimal.ZERO, 1, 1, 1))
                .isInstanceOf(InvalidIncomeInterruptionScenarioInputException.class);
    }

    @Test
    void rejectsNegativeInterruptedMonthlyNetIncome() {
        assertThatThrownBy(() -> calculator.calculate(
                "USD", BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("-0.01"), BigDecimal.ZERO, 1, 1, 1))
                .isInstanceOf(InvalidIncomeInterruptionScenarioInputException.class);
    }

    @Test
    void rejectsNegativeMonthlyExpenses() {
        assertThatThrownBy(() -> calculator.calculate(
                "USD", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("-0.01"), 1, 1, 1))
                .isInstanceOf(InvalidIncomeInterruptionScenarioInputException.class);
    }

    @Test
    void rejectsExcessiveFractionDigits() {
        assertThatThrownBy(() -> calculator.calculate(
                "USD", new BigDecimal("100.001"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 1, 1, 1))
                .isInstanceOf(InvalidIncomeInterruptionScenarioInputException.class);
    }

    @Test
    void rejectsExcessiveIntegerDigits() {
        BigDecimal tooLarge = new BigDecimal("100000000000000000.00");
        assertThatThrownBy(() -> calculator.calculate(
                "USD", tooLarge, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 1, 1, 1))
                .isInstanceOf(InvalidIncomeInterruptionScenarioInputException.class);
    }

    @Test
    void rejectsIntegerDigitLimitBypassedByNegativeScaleRepresentation() {
        BigDecimal negativeScaleTooLarge = new BigDecimal("1E+17");
        assertThatThrownBy(() -> calculator.calculate(
                "USD", negativeScaleTooLarge, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 1, 1, 1))
                .isInstanceOf(InvalidIncomeInterruptionScenarioInputException.class);
    }

    @Test
    void rejectsInterruptedIncomeExceedingNormalIncome() {
        assertThatThrownBy(() -> calculator.calculate(
                "USD", BigDecimal.ZERO, new BigDecimal("100.00"), new BigDecimal("100.01"),
                BigDecimal.ZERO, 1, 1, 1))
                .isInstanceOf(InvalidIncomeInterruptionScenarioInputException.class);
    }

    @Test
    void rejectsZeroHorizonMonths() {
        assertThatThrownBy(() -> calculator.calculate(
                "USD", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, 1, 1))
                .isInstanceOf(InvalidIncomeInterruptionScenarioInputException.class);
    }

    @Test
    void rejectsHorizonMonthsAboveTwelveHundred() {
        assertThatThrownBy(() -> calculator.calculate(
                "USD", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 1201, 1, 1))
                .isInstanceOf(InvalidIncomeInterruptionScenarioInputException.class);
    }

    @Test
    void rejectsInterruptionStartMonthBelowOne() {
        assertThatThrownBy(() -> calculator.calculate(
                "USD", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 3, 0, 1))
                .isInstanceOf(InvalidIncomeInterruptionScenarioInputException.class);
    }

    @Test
    void rejectsInterruptionStartMonthAboveHorizon() {
        assertThatThrownBy(() -> calculator.calculate(
                "USD", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 3, 4, 1))
                .isInstanceOf(InvalidIncomeInterruptionScenarioInputException.class);
    }

    @Test
    void rejectsInterruptionMonthsBelowOne() {
        assertThatThrownBy(() -> calculator.calculate(
                "USD", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 3, 1, 0))
                .isInstanceOf(InvalidIncomeInterruptionScenarioInputException.class);
    }

    @Test
    void rejectsIntervalThatExtendsBeyondTheHorizon() {
        assertThatThrownBy(() -> calculator.calculate(
                "USD", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 3, 3, 2))
                .isInstanceOf(InvalidIncomeInterruptionScenarioInputException.class);
    }

    @Test
    void acceptsHorizonMonthsAtTheLowerAndUpperBounds() {
        IncomeInterruptionScenarioResult lower = calculator.calculate(
                "USD", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 1, 1, 1);
        assertThat(lower.baselineRows()).hasSize(1);

        IncomeInterruptionScenarioResult upper = calculator.calculate(
                "USD", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 1200, 1, 1200);
        assertThat(upper.baselineRows()).hasSize(1200);
    }
}
