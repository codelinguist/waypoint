package com.waypoint.planning.multigoalfunding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.waypoint.planning.goalcontribution.GoalContributionCalculator;
import com.waypoint.planning.goalcontribution.GoalContributionStatus;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class MultiGoalFundingCheckCalculatorTest {

    private final MultiGoalFundingCheckCalculator calculator =
            new MultiGoalFundingCheckCalculator(new GoalContributionCalculator());

    @Test
    void computesShortfallForTheDocumentedTwoGoalExample() {
        MultiGoalFundingCheckResult result = calculator.calculate(
                "PHP",
                new BigDecimal("120"),
                List.of(
                        new GoalFundingInput("education", new BigDecimal("100"), BigDecimal.ZERO, 3),
                        new GoalFundingInput("travel", new BigDecimal("200"), BigDecimal.ZERO, 2)
                ));

        assertThat(result.goalResults()).hasSize(2);
        assertThat(result.goalResults().get(0).reference()).isEqualTo("education");
        assertThat(result.goalResults().get(0).contribution().monthlyContribution()).isEqualByComparingTo("33.34");
        assertThat(result.goalResults().get(1).reference()).isEqualTo("travel");
        assertThat(result.goalResults().get(1).contribution().monthlyContribution()).isEqualByComparingTo("100.00");
        assertThat(result.totalRequiredMonthlyContribution()).isEqualByComparingTo("133.34");
        assertThat(result.budgetMinusRequired()).isEqualByComparingTo("-13.34");
        assertThat(result.shortfall()).isEqualByComparingTo("13.34");
        assertThat(result.unallocatedBudget()).isEqualByComparingTo("0");
        assertThat(result.status()).isEqualTo(MultiGoalFundingStatus.SHORTFALL);
    }

    @Test
    void budgetExactlyEqualToTheTotalFits() {
        MultiGoalFundingCheckResult result = calculator.calculate(
                "PHP",
                new BigDecimal("100"),
                List.of(new GoalFundingInput("g1", new BigDecimal("300"), BigDecimal.ZERO, 3)));

        assertThat(result.totalRequiredMonthlyContribution()).isEqualByComparingTo("100.00");
        assertThat(result.budgetMinusRequired()).isEqualByComparingTo("0");
        assertThat(result.shortfall()).isEqualByComparingTo("0");
        assertThat(result.unallocatedBudget()).isEqualByComparingTo("0");
        assertThat(result.status()).isEqualTo(MultiGoalFundingStatus.FITS);
    }

    @Test
    void unallocatedBudgetIsReturnedWhenTheBudgetExceedsTheTotal() {
        MultiGoalFundingCheckResult result = calculator.calculate(
                "PHP",
                new BigDecimal("500"),
                List.of(new GoalFundingInput("g1", new BigDecimal("300"), BigDecimal.ZERO, 3)));

        assertThat(result.unallocatedBudget()).isEqualByComparingTo("400.00");
        assertThat(result.shortfall()).isEqualByComparingTo("0");
        assertThat(result.status()).isEqualTo(MultiGoalFundingStatus.FITS);
    }

    @Test
    void alreadyFundedGoalsContributeZeroRequirementAndRetainStatus() {
        MultiGoalFundingCheckResult result = calculator.calculate(
                "PHP",
                BigDecimal.ZERO,
                List.of(new GoalFundingInput("g1", new BigDecimal("100"), new BigDecimal("150"), 12)));

        assertThat(result.goalResults().get(0).contribution().status())
                .isEqualTo(GoalContributionStatus.ALREADY_FUNDED);
        assertThat(result.goalResults().get(0).contribution().monthlyContribution()).isEqualByComparingTo("0");
        assertThat(result.totalRequiredMonthlyContribution()).isEqualByComparingTo("0");
        assertThat(result.status()).isEqualTo(MultiGoalFundingStatus.FITS);
    }

    @Test
    void zeroBudgetIsValidAndProducesShortfallWhenAnyContributionIsRequired() {
        MultiGoalFundingCheckResult result = calculator.calculate(
                "PHP",
                BigDecimal.ZERO,
                List.of(new GoalFundingInput("g1", new BigDecimal("100"), BigDecimal.ZERO, 10)));

        assertThat(result.status()).isEqualTo(MultiGoalFundingStatus.SHORTFALL);
        assertThat(result.shortfall()).isEqualByComparingTo(result.totalRequiredMonthlyContribution());
    }

    @Test
    void preservesCallerOrderAndDifferingContributionMonthsPerGoal() {
        MultiGoalFundingCheckResult result = calculator.calculate(
                "PHP",
                new BigDecimal("1000"),
                List.of(
                        new GoalFundingInput("third", new BigDecimal("30"), BigDecimal.ZERO, 1),
                        new GoalFundingInput("first", new BigDecimal("10"), BigDecimal.ZERO, 5),
                        new GoalFundingInput("second", new BigDecimal("20"), BigDecimal.ZERO, 2)
                ));

        assertThat(result.goalResults()).extracting(GoalFundingCheckItemResult::reference)
                .containsExactly("third", "first", "second");
    }

    @Test
    void sumsRoundedPerGoalContributionsRatherThanRoundingAnAggregateQuotient() {
        MultiGoalFundingCheckResult result = calculator.calculate(
                "PHP",
                new BigDecimal("1000"),
                List.of(
                        new GoalFundingInput("g1", new BigDecimal("100"), BigDecimal.ZERO, 3),
                        new GoalFundingInput("g2", new BigDecimal("100"), BigDecimal.ZERO, 3),
                        new GoalFundingInput("g3", new BigDecimal("100"), BigDecimal.ZERO, 3)
                ));

        assertThat(result.totalRequiredMonthlyContribution()).isEqualByComparingTo("100.02");
    }

    @Test
    void normalizesCurrencyToUppercase() {
        MultiGoalFundingCheckResult result = calculator.calculate(
                "php",
                new BigDecimal("100"),
                List.of(new GoalFundingInput("g1", new BigDecimal("100"), BigDecimal.ZERO, 1)));

        assertThat(result.currency()).isEqualTo("PHP");
    }

    @Test
    void identicalInputsProduceIdenticalResults() {
        List<GoalFundingInput> goals = List.of(new GoalFundingInput("g1", new BigDecimal("100"), BigDecimal.ZERO, 3));

        MultiGoalFundingCheckResult first = calculator.calculate("PHP", new BigDecimal("100"), goals);
        MultiGoalFundingCheckResult second = calculator.calculate("PHP", new BigDecimal("100"), goals);

        assertThat(first).isEqualTo(second);
    }

    @Test
    void rejectsNullCurrency() {
        assertThatThrownBy(() -> calculator.calculate(
                null, BigDecimal.ZERO, List.of(new GoalFundingInput("g1", BigDecimal.ONE, BigDecimal.ZERO, 1))))
                .isInstanceOf(InvalidMultiGoalFundingInputException.class);
    }

    @Test
    void rejectsMalformedCurrencyCode() {
        assertThatThrownBy(() -> calculator.calculate(
                "PH", BigDecimal.ZERO, List.of(new GoalFundingInput("g1", BigDecimal.ONE, BigDecimal.ZERO, 1))))
                .isInstanceOf(InvalidMultiGoalFundingInputException.class);
    }

    @Test
    void rejectsNullAvailableMonthlyBudget() {
        assertThatThrownBy(() -> calculator.calculate(
                "PHP", null, List.of(new GoalFundingInput("g1", BigDecimal.ONE, BigDecimal.ZERO, 1))))
                .isInstanceOf(InvalidMultiGoalFundingInputException.class);
    }

    @Test
    void rejectsNegativeAvailableMonthlyBudget() {
        assertThatThrownBy(() -> calculator.calculate(
                "PHP", new BigDecimal("-1"), List.of(new GoalFundingInput("g1", BigDecimal.ONE, BigDecimal.ZERO, 1))))
                .isInstanceOf(InvalidMultiGoalFundingInputException.class);
    }

    @Test
    void rejectsExcessiveIntegerDigitsOnAvailableMonthlyBudget() {
        assertThatThrownBy(() -> calculator.calculate(
                "PHP", new BigDecimal("100000000000000000"),
                List.of(new GoalFundingInput("g1", BigDecimal.ONE, BigDecimal.ZERO, 1))))
                .isInstanceOf(InvalidMultiGoalFundingInputException.class);
    }

    @Test
    void rejectsEmptyGoalsList() {
        assertThatThrownBy(() -> calculator.calculate("PHP", BigDecimal.ZERO, List.of()))
                .isInstanceOf(InvalidMultiGoalFundingInputException.class);
    }

    @Test
    void rejectsNullGoalsList() {
        assertThatThrownBy(() -> calculator.calculate("PHP", BigDecimal.ZERO, null))
                .isInstanceOf(InvalidMultiGoalFundingInputException.class);
    }

    @Test
    void rejectsMoreThanFiftyGoals() {
        List<GoalFundingInput> tooMany = java.util.stream.IntStream.range(0, 51)
                .mapToObj(i -> new GoalFundingInput("g" + i, BigDecimal.ONE, BigDecimal.ZERO, 1))
                .toList();

        assertThatThrownBy(() -> calculator.calculate("PHP", BigDecimal.ZERO, tooMany))
                .isInstanceOf(InvalidMultiGoalFundingInputException.class);
    }

    @Test
    void acceptsExactlyFiftyGoals() {
        List<GoalFundingInput> fifty = java.util.stream.IntStream.range(0, 50)
                .mapToObj(i -> new GoalFundingInput("g" + i, BigDecimal.ONE, BigDecimal.ZERO, 1))
                .toList();

        MultiGoalFundingCheckResult result = calculator.calculate("PHP", new BigDecimal("1000"), fifty);

        assertThat(result.goalResults()).hasSize(50);
    }

    @Test
    void rejectsNullGoalEntry() {
        List<GoalFundingInput> goals = new java.util.ArrayList<>();
        goals.add(new GoalFundingInput("g1", BigDecimal.ONE, BigDecimal.ZERO, 1));
        goals.add(null);

        assertThatThrownBy(() -> calculator.calculate("PHP", BigDecimal.ZERO, goals))
                .isInstanceOf(InvalidMultiGoalFundingInputException.class);
    }

    @Test
    void rejectsBlankReference() {
        assertThatThrownBy(() -> calculator.calculate(
                "PHP", BigDecimal.ZERO, List.of(new GoalFundingInput(" ", BigDecimal.ONE, BigDecimal.ZERO, 1))))
                .isInstanceOf(InvalidMultiGoalFundingInputException.class);
    }

    @Test
    void rejectsReferenceLongerThanSixtyFourCharacters() {
        String tooLong = "r".repeat(65);
        assertThatThrownBy(() -> calculator.calculate(
                "PHP", BigDecimal.ZERO, List.of(new GoalFundingInput(tooLong, BigDecimal.ONE, BigDecimal.ZERO, 1))))
                .isInstanceOf(InvalidMultiGoalFundingInputException.class);
    }

    @Test
    void rejectsDuplicateReferences() {
        assertThatThrownBy(() -> calculator.calculate(
                "PHP",
                new BigDecimal("1000"),
                List.of(
                        new GoalFundingInput("dup", BigDecimal.ONE, BigDecimal.ZERO, 1),
                        new GoalFundingInput("dup", BigDecimal.TEN, BigDecimal.ZERO, 1)
                )))
                .isInstanceOf(InvalidMultiGoalFundingInputException.class)
                .hasMessageContaining("dup");
    }

    @Test
    void rejectsNullTargetAmountForAGoalAndIdentifiesItsReference() {
        assertThatThrownBy(() -> calculator.calculate(
                "PHP", BigDecimal.ZERO, List.of(new GoalFundingInput("g1", null, BigDecimal.ZERO, 1))))
                .isInstanceOf(InvalidMultiGoalFundingInputException.class)
                .hasMessageContaining("g1");
    }

    @Test
    void rejectsZeroTargetAmountForAGoal() {
        assertThatThrownBy(() -> calculator.calculate(
                "PHP", BigDecimal.ZERO, List.of(new GoalFundingInput("g1", BigDecimal.ZERO, BigDecimal.ZERO, 1))))
                .isInstanceOf(InvalidMultiGoalFundingInputException.class);
    }

    @Test
    void rejectsNegativeCurrentAmountForAGoal() {
        assertThatThrownBy(() -> calculator.calculate(
                "PHP", BigDecimal.ZERO,
                List.of(new GoalFundingInput("g1", BigDecimal.ONE, new BigDecimal("-1"), 1))))
                .isInstanceOf(InvalidMultiGoalFundingInputException.class);
    }

    @Test
    void rejectsOutOfRangeContributionMonthsForAGoal() {
        assertThatThrownBy(() -> calculator.calculate(
                "PHP", BigDecimal.ZERO, List.of(new GoalFundingInput("g1", BigDecimal.ONE, BigDecimal.ZERO, 0))))
                .isInstanceOf(InvalidMultiGoalFundingInputException.class);
        assertThatThrownBy(() -> calculator.calculate(
                "PHP", BigDecimal.ZERO, List.of(new GoalFundingInput("g1", BigDecimal.ONE, BigDecimal.ZERO, 1201))))
                .isInstanceOf(InvalidMultiGoalFundingInputException.class);
    }

    @Test
    void rejectsIntegerDigitLimitBypassedByNegativeScaleRepresentationOnAGoalAmount() {
        BigDecimal negativeScaleTooLarge = new BigDecimal("1E+17");
        assertThatThrownBy(() -> calculator.calculate(
                "PHP", BigDecimal.ZERO,
                List.of(new GoalFundingInput("g1", negativeScaleTooLarge, BigDecimal.ZERO, 1))))
                .isInstanceOf(InvalidMultiGoalFundingInputException.class);
    }
}
