package com.waypoint.assumption;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.waypoint.household.Household;
import com.waypoint.household.HouseholdNotFoundException;
import com.waypoint.household.HouseholdRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlanningAssumptionServiceTest {

    private final HouseholdRepository householdRepository = mock(HouseholdRepository.class);
    private final PlanningAssumptionRepository planningAssumptionRepository = mock(PlanningAssumptionRepository.class);
    private final PlanningAssumptionService service =
            new PlanningAssumptionService(householdRepository, planningAssumptionRepository);

    @Test
    void throwsNotFoundWhenCreatingForUnknownHousehold() {
        UUID householdId = UUID.randomUUID();
        when(householdRepository.findById(householdId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createAssumption(
                householdId, "Expected return", "0.06", "PERCENTAGE_ANNUAL", null,
                LocalDate.now(), null, LocalDate.now().plusYears(1)))
                .isInstanceOf(HouseholdNotFoundException.class);
    }

    @Test
    void createsAssumptionWithTrimmedFieldsAndBlankNotesNormalizedToNull() {
        UUID householdId = UUID.randomUUID();
        Household household = new Household("Ralph Household", "PHP");
        when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));
        when(planningAssumptionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PlanningAssumption assumption = service.createAssumption(
                householdId, "  Expected return  ", "  0.06  ", "  PERCENTAGE_ANNUAL  ", "   ",
                LocalDate.now(), null, LocalDate.now().plusYears(1));

        assertThat(assumption.getName()).isEqualTo("Expected return");
        assertThat(assumption.getValue()).isEqualTo("0.06");
        assertThat(assumption.getValueType()).isEqualTo("PERCENTAGE_ANNUAL");
        assertThat(assumption.getNotes()).isNull();
        assertThat(assumption.getHousehold()).isSameAs(household);
        assertThat(assumption.getSupersededBy()).isNull();
    }

    @Test
    void rejectsEffectiveUntilBeforeEffectiveFrom() {
        UUID householdId = UUID.randomUUID();
        when(householdRepository.findById(householdId)).thenReturn(Optional.of(new Household("H", "PHP")));

        assertThatThrownBy(() -> service.createAssumption(
                householdId, "Expected return", "0.06", "PERCENTAGE_ANNUAL", null,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1)))
                .isInstanceOf(InvalidPlanningAssumptionException.class);
        verify(planningAssumptionRepository, never()).save(any());
    }

    @Test
    void throwsNotFoundWhenGettingForUnknownHousehold() {
        UUID householdId = UUID.randomUUID();
        when(householdRepository.existsById(householdId)).thenReturn(false);

        assertThatThrownBy(() -> service.getAssumption(householdId, UUID.randomUUID()))
                .isInstanceOf(HouseholdNotFoundException.class);
    }

    @Test
    void throwsNotFoundWhenAssumptionBelongsToAnotherHousehold() {
        UUID householdId = UUID.randomUUID();
        UUID assumptionId = UUID.randomUUID();
        when(householdRepository.existsById(householdId)).thenReturn(true);
        when(planningAssumptionRepository.findByIdAndHousehold_Id(assumptionId, householdId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAssumption(householdId, assumptionId))
                .isInstanceOf(PlanningAssumptionNotFoundException.class);
    }

    @Test
    void throwsNotFoundWhenListingForUnknownHousehold() {
        UUID householdId = UUID.randomUUID();
        when(householdRepository.existsById(householdId)).thenReturn(false);

        assertThatThrownBy(() -> service.listAssumptions(householdId))
                .isInstanceOf(HouseholdNotFoundException.class);
    }

    @Test
    void listActiveAssumptionsFiltersOutSupersededAndOutOfWindowVersions() {
        UUID householdId = UUID.randomUUID();
        Household household = new Household("Ralph Household", "PHP");
        when(householdRepository.existsById(householdId)).thenReturn(true);

        PlanningAssumption active = new PlanningAssumption(
                household, "Expected return", "0.06", "PERCENTAGE_ANNUAL", null,
                LocalDate.of(2026, 1, 1), null, LocalDate.of(2027, 1, 1));
        PlanningAssumption notYetEffective = new PlanningAssumption(
                household, "Tuition inflation", "0.05", "PERCENTAGE_ANNUAL", null,
                LocalDate.of(2030, 1, 1), null, LocalDate.of(2031, 1, 1));
        PlanningAssumption superseded = new PlanningAssumption(
                household, "Old assumption", "0.04", "PERCENTAGE_ANNUAL", null,
                LocalDate.of(2020, 1, 1), null, LocalDate.of(2021, 1, 1));
        PlanningAssumption replacement = new PlanningAssumption(
                household, "Old assumption", "0.045", "PERCENTAGE_ANNUAL", null,
                LocalDate.of(2020, 1, 1), null, LocalDate.of(2021, 1, 1));
        superseded.linkSupersededBy(replacement);

        when(planningAssumptionRepository.findByHousehold_IdOrderByNameAscEffectiveFromAscCreatedAtAscIdAsc(householdId))
                .thenReturn(List.of(active, notYetEffective, superseded));

        List<PlanningAssumption> result = service.listActiveAssumptions(householdId, LocalDate.of(2026, 6, 1));

        assertThat(result).containsExactly(active);
    }

    @Test
    void supersedeCreatesReplacementAndLinksPriorVersion() {
        UUID householdId = UUID.randomUUID();
        UUID assumptionId = UUID.randomUUID();
        Household household = new Household("Ralph Household", "PHP");
        when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));
        PlanningAssumption prior = new PlanningAssumption(
                household, "Expected return", "0.06", "PERCENTAGE_ANNUAL", null,
                LocalDate.of(2026, 1, 1), null, LocalDate.of(2027, 1, 1));
        when(planningAssumptionRepository.findByIdAndHousehold_Id(assumptionId, householdId))
                .thenReturn(Optional.of(prior));
        when(planningAssumptionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PlanningAssumption replacement = service.supersedeAssumption(
                householdId, assumptionId, "Expected return", "0.07", "PERCENTAGE_ANNUAL", "revised outlook",
                LocalDate.of(2027, 1, 1), null, LocalDate.of(2028, 1, 1));

        assertThat(replacement.getValue()).isEqualTo("0.07");
        assertThat(prior.getSupersededBy()).isSameAs(replacement);
    }

    @Test
    void rejectsSupersedingAnAlreadySupersededAssumption() {
        UUID householdId = UUID.randomUUID();
        UUID assumptionId = UUID.randomUUID();
        Household household = new Household("Ralph Household", "PHP");
        when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));
        PlanningAssumption prior = new PlanningAssumption(
                household, "Expected return", "0.06", "PERCENTAGE_ANNUAL", null,
                LocalDate.of(2026, 1, 1), null, LocalDate.of(2027, 1, 1));
        PlanningAssumption existingReplacement = new PlanningAssumption(
                household, "Expected return", "0.065", "PERCENTAGE_ANNUAL", null,
                LocalDate.of(2026, 1, 1), null, LocalDate.of(2027, 1, 1));
        prior.linkSupersededBy(existingReplacement);
        when(planningAssumptionRepository.findByIdAndHousehold_Id(assumptionId, householdId))
                .thenReturn(Optional.of(prior));

        assertThatThrownBy(() -> service.supersedeAssumption(
                householdId, assumptionId, "Expected return", "0.07", "PERCENTAGE_ANNUAL", null,
                LocalDate.of(2027, 1, 1), null, LocalDate.of(2028, 1, 1)))
                .isInstanceOf(InvalidPlanningAssumptionException.class);
        verify(planningAssumptionRepository, never()).save(any());
    }

    @Test
    void rejectsSupersedingWithADifferentLogicalName() {
        UUID householdId = UUID.randomUUID();
        UUID assumptionId = UUID.randomUUID();
        Household household = new Household("Ralph Household", "PHP");
        when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));
        PlanningAssumption prior = new PlanningAssumption(
                household, "Expected return", "0.06", "PERCENTAGE_ANNUAL", null,
                LocalDate.of(2026, 1, 1), null, LocalDate.of(2027, 1, 1));
        when(planningAssumptionRepository.findByIdAndHousehold_Id(assumptionId, householdId))
                .thenReturn(Optional.of(prior));

        assertThatThrownBy(() -> service.supersedeAssumption(
                householdId, assumptionId, "A different assumption", "0.07", "PERCENTAGE_ANNUAL", null,
                LocalDate.of(2027, 1, 1), null, LocalDate.of(2028, 1, 1)))
                .isInstanceOf(InvalidPlanningAssumptionException.class);
        verify(planningAssumptionRepository, never()).save(any());
        assertThat(prior.getSupersededBy()).isNull();
    }

    @Test
    void rejectsSupersedingWithInvalidDateOrderingWithoutMutatingPriorVersion() {
        UUID householdId = UUID.randomUUID();
        UUID assumptionId = UUID.randomUUID();
        Household household = new Household("Ralph Household", "PHP");
        when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));
        PlanningAssumption prior = new PlanningAssumption(
                household, "Expected return", "0.06", "PERCENTAGE_ANNUAL", null,
                LocalDate.of(2026, 1, 1), null, LocalDate.of(2027, 1, 1));
        when(planningAssumptionRepository.findByIdAndHousehold_Id(assumptionId, householdId))
                .thenReturn(Optional.of(prior));

        assertThatThrownBy(() -> service.supersedeAssumption(
                householdId, assumptionId, "Expected return", "0.07", "PERCENTAGE_ANNUAL", null,
                LocalDate.of(2027, 6, 1), LocalDate.of(2027, 1, 1), LocalDate.of(2028, 1, 1)))
                .isInstanceOf(InvalidPlanningAssumptionException.class);
        verify(planningAssumptionRepository, never()).save(any());
        assertThat(prior.getSupersededBy()).isNull();
    }

    @Test
    void throwsNotFoundWhenSupersedingForUnknownHousehold() {
        UUID householdId = UUID.randomUUID();
        when(householdRepository.findById(householdId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.supersedeAssumption(
                householdId, UUID.randomUUID(), "Expected return", "0.07", "PERCENTAGE_ANNUAL", null,
                LocalDate.now(), null, LocalDate.now().plusYears(1)))
                .isInstanceOf(HouseholdNotFoundException.class);
    }
}
