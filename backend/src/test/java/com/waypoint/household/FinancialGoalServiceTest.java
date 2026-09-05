package com.waypoint.household;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FinancialGoalServiceTest {

    private final HouseholdRepository householdRepository = mock(HouseholdRepository.class);
    private final FinancialGoalRepository financialGoalRepository = mock(FinancialGoalRepository.class);
    private final FinancialGoalService financialGoalService =
            new FinancialGoalService(householdRepository, financialGoalRepository);

    @Test
    void throwsNotFoundWhenCreatingGoalForUnknownHousehold() {
        UUID householdId = UUID.randomUUID();
        when(householdRepository.findById(householdId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> financialGoalService.createGoal(
                householdId, "Retirement", BigDecimal.TEN, "PHP", LocalDate.now().plusYears(1), 1, BigDecimal.ZERO))
                .isInstanceOf(HouseholdNotFoundException.class);
    }

    @Test
    void createsGoalWithTrimmedNameAndNormalizedCurrency() {
        UUID householdId = UUID.randomUUID();
        Household household = new Household("Ralph Household", "PHP");
        when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));
        when(financialGoalRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        FinancialGoal goal = financialGoalService.createGoal(
                householdId, "  Emergency Fund  ", new BigDecimal("1000.00"), "php",
                LocalDate.now().plusMonths(6), 1, new BigDecimal("250.00"));

        assertThat(goal.getName()).isEqualTo("Emergency Fund");
        assertThat(goal.getCurrency()).isEqualTo("PHP");
        assertThat(goal.getHousehold()).isSameAs(household);
    }

    @Test
    void throwsNotFoundWhenGettingGoalForUnknownHousehold() {
        UUID householdId = UUID.randomUUID();
        when(householdRepository.existsById(householdId)).thenReturn(false);

        assertThatThrownBy(() -> financialGoalService.getGoal(householdId, UUID.randomUUID()))
                .isInstanceOf(HouseholdNotFoundException.class);
    }

    @Test
    void throwsNotFoundWhenGoalBelongsToAnotherHousehold() {
        UUID householdId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        when(householdRepository.existsById(householdId)).thenReturn(true);
        when(financialGoalRepository.findByIdAndHousehold_Id(goalId, householdId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> financialGoalService.getGoal(householdId, goalId))
                .isInstanceOf(FinancialGoalNotFoundException.class);
    }

    @Test
    void throwsNotFoundWhenListingGoalsForUnknownHousehold() {
        UUID householdId = UUID.randomUUID();
        when(householdRepository.existsById(householdId)).thenReturn(false);

        assertThatThrownBy(() -> financialGoalService.listGoals(householdId))
                .isInstanceOf(HouseholdNotFoundException.class);
    }

    @Test
    void listsGoalsOrderedByPriorityForKnownHousehold() {
        UUID householdId = UUID.randomUUID();
        when(householdRepository.existsById(householdId)).thenReturn(true);
        when(financialGoalRepository.findByHousehold_IdOrderByPriorityAscCreatedAtAscIdAsc(householdId))
                .thenReturn(List.of());

        assertThat(financialGoalService.listGoals(householdId)).isEmpty();
    }

    @Test
    void computesRemainingAmountAsTargetMinusCurrent() {
        Household household = new Household("Ralph Household", "PHP");
        FinancialGoal goal = new FinancialGoal(household, "Retirement", new BigDecimal("1000.00"), "PHP",
                LocalDate.now().plusYears(1), 1, new BigDecimal("400.00"));

        assertThat(FinancialGoalService.remainingAmount(goal)).isEqualByComparingTo("600.00");
    }

    @Test
    void allowsNegativeRemainingAmountWhenGoalIsOverachieved() {
        Household household = new Household("Ralph Household", "PHP");
        FinancialGoal goal = new FinancialGoal(household, "Retirement", new BigDecimal("1000.00"), "PHP",
                LocalDate.now().plusYears(1), 1, new BigDecimal("1200.00"));

        assertThat(FinancialGoalService.remainingAmount(goal)).isEqualByComparingTo("-200.00");
    }

    @Test
    void computesProgressPercentageFromCurrentAndTargetAmounts() {
        Household household = new Household("Ralph Household", "PHP");
        FinancialGoal goal = new FinancialGoal(household, "Retirement", new BigDecimal("1000.00"), "PHP",
                LocalDate.now().plusYears(1), 1, new BigDecimal("250.00"));

        assertThat(FinancialGoalService.progressPercentage(goal)).isEqualByComparingTo("25.00");
    }

    @Test
    void boundsProgressPercentageAtOneHundredWhenCurrentExceedsTarget() {
        Household household = new Household("Ralph Household", "PHP");
        FinancialGoal goal = new FinancialGoal(household, "Retirement", new BigDecimal("1000.00"), "PHP",
                LocalDate.now().plusYears(1), 1, new BigDecimal("1500.00"));

        assertThat(FinancialGoalService.progressPercentage(goal)).isEqualByComparingTo("100.00");
    }

    @Test
    void boundsProgressPercentageAtZeroWhenCurrentIsZero() {
        Household household = new Household("Ralph Household", "PHP");
        FinancialGoal goal = new FinancialGoal(household, "Retirement", new BigDecimal("1000.00"), "PHP",
                LocalDate.now().plusYears(1), 1, BigDecimal.ZERO);

        assertThat(FinancialGoalService.progressPercentage(goal)).isEqualByComparingTo("0.00");
    }

    @Test
    void doesNotMutateStoredAmountsWhenComputingProgress() {
        Household household = new Household("Ralph Household", "PHP");
        FinancialGoal goal = new FinancialGoal(household, "Retirement", new BigDecimal("1000.00"), "PHP",
                LocalDate.now().plusYears(1), 1, new BigDecimal("1500.00"));

        FinancialGoalService.progressPercentage(goal);

        assertThat(goal.getCurrentAmount()).isEqualByComparingTo("1500.00");
        assertThat(goal.getTargetAmount()).isEqualByComparingTo("1000.00");
    }
}
