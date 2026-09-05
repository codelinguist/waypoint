package com.waypoint.planning.goalcontribution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GoalContributionCalculatorTest {

    private final GoalContributionCalculator calculator = new GoalContributionCalculator();
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
    void calculatesEqualMonthlyContributionsThatExactlyReachTheTarget() {
        GoalContributionResult result = calculator.calculate(
                "PHP", new BigDecimal("1000.00"), new BigDecimal("100.00"), 3);

        assertThat(result.currency()).isEqualTo("PHP");
        assertThat(result.remainingAmount()).isEqualByComparingTo("900.00");
        assertThat(result.monthlyContribution()).isEqualByComparingTo("300.00");
        assertThat(result.totalContributions()).isEqualByComparingTo("900.00");
        assertThat(result.projectedAmount()).isEqualByComparingTo("1000.00");
        assertThat(result.amountAboveTarget()).isEqualByComparingTo("0");
        assertThat(result.status()).isEqualTo(GoalContributionStatus.CONTRIBUTIONS_REQUIRED);
    }

    @Test
    void roundsMonthlyContributionUpSoTotalContributionsNeverFallShortOfTheGap() {
        GoalContributionResult result = calculator.calculate(
                "PHP", new BigDecimal("100.00"), new BigDecimal("0"), 3);

        assertThat(result.monthlyContribution()).isEqualByComparingTo("33.34");
        assertThat(result.totalContributions()).isEqualByComparingTo("100.02");
        assertThat(result.projectedAmount()).isEqualByComparingTo("100.02");
        assertThat(result.amountAboveTarget()).isEqualByComparingTo("0.02");
        assertThat(result.status()).isEqualTo(GoalContributionStatus.CONTRIBUTIONS_REQUIRED);
    }

    @Test
    void reconcilesProjectedAmountWithCurrentAmountPlusTotalContributionsForAnArbitraryGap() {
        GoalContributionResult result = calculator.calculate(
                "USD", new BigDecimal("777.77"), new BigDecimal("123.45"), 11);

        assertThat(result.projectedAmount())
                .isEqualByComparingTo(result.currentAmount().add(result.totalContributions()));
        assertThat(result.projectedAmount()).isGreaterThanOrEqualTo(result.targetAmount());
    }

    @Test
    void returnsAlreadyFundedWhenCurrentAmountEqualsTarget() {
        GoalContributionResult result = calculator.calculate(
                "PHP", new BigDecimal("500.00"), new BigDecimal("500.00"), 12);

        assertThat(result.status()).isEqualTo(GoalContributionStatus.ALREADY_FUNDED);
        assertThat(result.remainingAmount()).isEqualByComparingTo("0");
        assertThat(result.monthlyContribution()).isEqualByComparingTo("0");
        assertThat(result.totalContributions()).isEqualByComparingTo("0");
        assertThat(result.projectedAmount()).isEqualByComparingTo("500.00");
        assertThat(result.amountAboveTarget()).isEqualByComparingTo("0");
    }

    @Test
    void returnsAlreadyFundedAndPreservesExistingSurplusWhenCurrentAmountExceedsTarget() {
        GoalContributionResult result = calculator.calculate(
                "PHP", new BigDecimal("500.00"), new BigDecimal("650.00"), 12);

        assertThat(result.status()).isEqualTo(GoalContributionStatus.ALREADY_FUNDED);
        assertThat(result.remainingAmount()).isEqualByComparingTo("0");
        assertThat(result.monthlyContribution()).isEqualByComparingTo("0");
        assertThat(result.totalContributions()).isEqualByComparingTo("0");
        assertThat(result.projectedAmount()).isEqualByComparingTo("650.00");
        assertThat(result.amountAboveTarget()).isEqualByComparingTo("150.00");
    }

    @Test
    void oneMonthReturnsTheEntirePositiveGap() {
        GoalContributionResult result = calculator.calculate(
                "PHP", new BigDecimal("250.75"), new BigDecimal("10.00"), 1);

        assertThat(result.monthlyContribution()).isEqualByComparingTo("240.75");
        assertThat(result.totalContributions()).isEqualByComparingTo("240.75");
        assertThat(result.projectedAmount()).isEqualByComparingTo("250.75");
        assertThat(result.amountAboveTarget()).isEqualByComparingTo("0");
    }

    @Test
    void normalizesCurrencyToUppercase() {
        GoalContributionResult result = calculator.calculate(
                "php", new BigDecimal("100.00"), new BigDecimal("0"), 1);

        assertThat(result.currency()).isEqualTo("PHP");
    }

    @Test
    void rejectsNullCurrency() {
        assertThatThrownBy(() -> calculator.calculate(null, new BigDecimal("100.00"), BigDecimal.ZERO, 1))
                .isInstanceOf(InvalidGoalContributionInputException.class);
    }

    @Test
    void rejectsMalformedCurrencyCode() {
        assertThatThrownBy(() -> calculator.calculate("PH", new BigDecimal("100.00"), BigDecimal.ZERO, 1))
                .isInstanceOf(InvalidGoalContributionInputException.class);
        assertThatThrownBy(() -> calculator.calculate("PHP1", new BigDecimal("100.00"), BigDecimal.ZERO, 1))
                .isInstanceOf(InvalidGoalContributionInputException.class);
    }

    @Test
    void rejectsNullTargetAmount() {
        assertThatThrownBy(() -> calculator.calculate("PHP", null, BigDecimal.ZERO, 1))
                .isInstanceOf(InvalidGoalContributionInputException.class);
    }

    @Test
    void rejectsZeroOrNegativeTargetAmount() {
        assertThatThrownBy(() -> calculator.calculate("PHP", BigDecimal.ZERO, BigDecimal.ZERO, 1))
                .isInstanceOf(InvalidGoalContributionInputException.class);
        assertThatThrownBy(() -> calculator.calculate(
                "PHP", new BigDecimal("-1.00"), BigDecimal.ZERO, 1))
                .isInstanceOf(InvalidGoalContributionInputException.class);
    }

    @Test
    void rejectsNegativeCurrentAmount() {
        assertThatThrownBy(() -> calculator.calculate(
                "PHP", new BigDecimal("100.00"), new BigDecimal("-0.01"), 1))
                .isInstanceOf(InvalidGoalContributionInputException.class);
    }

    @Test
    void rejectsExcessiveFractionDigits() {
        assertThatThrownBy(() -> calculator.calculate(
                "PHP", new BigDecimal("100.001"), BigDecimal.ZERO, 1))
                .isInstanceOf(InvalidGoalContributionInputException.class);
    }

    @Test
    void rejectsExcessiveIntegerDigits() {
        BigDecimal tooLarge = new BigDecimal("100000000000000000.00");
        assertThatThrownBy(() -> calculator.calculate("PHP", tooLarge, BigDecimal.ZERO, 1))
                .isInstanceOf(InvalidGoalContributionInputException.class);
    }

    @Test
    void rejectsIntegerDigitLimitBypassedByNegativeScaleRepresentation() {
        BigDecimal negativeScaleTooLarge = new BigDecimal("1E+17");
        assertThatThrownBy(() -> calculator.calculate("PHP", negativeScaleTooLarge, BigDecimal.ZERO, 1))
                .isInstanceOf(InvalidGoalContributionInputException.class);
        assertThatThrownBy(() -> calculator.calculate("PHP", new BigDecimal("100.00"), negativeScaleTooLarge, 1))
                .isInstanceOf(InvalidGoalContributionInputException.class);
    }

    @Test
    void acceptsIntegerDigitLimitAtTheNegativeScaleBoundary() {
        GoalContributionResult result = calculator.calculate(
                "PHP", new BigDecimal("1E+16"), BigDecimal.ZERO, 1);

        assertThat(result.targetAmount()).isEqualByComparingTo("1E+16");
        assertThat(result.status()).isEqualTo(GoalContributionStatus.CONTRIBUTIONS_REQUIRED);
    }

    @Test
    void normalizesCurrencyToUppercaseIndependentlyOfDefaultLocale() {
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));

        GoalContributionResult result = calculator.calculate(
                "inr", new BigDecimal("100.00"), new BigDecimal("0"), 1);

        assertThat(result.currency()).isEqualTo("INR");
    }

    @Test
    void rejectsZeroContributionMonths() {
        assertThatThrownBy(() -> calculator.calculate(
                "PHP", new BigDecimal("100.00"), BigDecimal.ZERO, 0))
                .isInstanceOf(InvalidGoalContributionInputException.class);
    }

    @Test
    void rejectsNegativeContributionMonths() {
        assertThatThrownBy(() -> calculator.calculate(
                "PHP", new BigDecimal("100.00"), BigDecimal.ZERO, -1))
                .isInstanceOf(InvalidGoalContributionInputException.class);
    }

    @Test
    void rejectsContributionMonthsAboveTwelveHundred() {
        assertThatThrownBy(() -> calculator.calculate(
                "PHP", new BigDecimal("100.00"), BigDecimal.ZERO, 1201))
                .isInstanceOf(InvalidGoalContributionInputException.class);
    }

    @Test
    void acceptsContributionMonthsAtTheUpperBound() {
        GoalContributionResult result = calculator.calculate(
                "PHP", new BigDecimal("1200.00"), BigDecimal.ZERO, 1200);

        assertThat(result.monthlyContribution()).isEqualByComparingTo("1.00");
        assertThat(result.status()).isEqualTo(GoalContributionStatus.CONTRIBUTIONS_REQUIRED);
    }
}
