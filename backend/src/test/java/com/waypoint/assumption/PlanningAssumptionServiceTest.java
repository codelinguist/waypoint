package com.waypoint.assumption;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.waypoint.household.Household;
import com.waypoint.household.HouseholdNotFoundException;
import com.waypoint.household.HouseholdRepository;
import com.waypoint.household.SourceType;
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
                householdId, "Future monthly income", "150000", "PHP/month", null,
                LocalDate.now(), null, LocalDate.now().plusMonths(6)))
                .isInstanceOf(HouseholdNotFoundException.class);
    }

    @Test
    void createsAssumptionWithTrimmedFieldsBlankNotesAsNullAndManualEntryProvenance() {
        UUID householdId = UUID.randomUUID();
        Household household = new Household("Ralph Household", "PHP");
        when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));
        when(planningAssumptionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PlanningAssumption assumption = service.createAssumption(
                householdId, "  Future monthly income  ", "  150000  ", "  PHP/month  ", "   ",
                LocalDate.now(), null, LocalDate.now().plusMonths(6));

        assertThat(assumption.getName()).isEqualTo("Future monthly income");
        assertThat(assumption.getValue()).isEqualTo("150000");
        assertThat(assumption.getValueType()).isEqualTo("PHP/month");
        assertThat(assumption.getNotes()).isNull();
        assertThat(assumption.getSourceType()).isEqualTo(SourceType.MANUAL_ENTRY);
        assertThat(assumption.getSupersededById()).isNull();
        assertThat(assumption.getHousehold()).isSameAs(household);
    }

    @Test
    void rejectsEffectiveUntilBeforeEffectiveFromOnCreate() {
        UUID householdId = UUID.randomUUID();
        Household household = new Household("Ralph Household", "PHP");
        when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));

        assertThatThrownBy(() -> service.createAssumption(
                householdId, "Future monthly income", "150000", "PHP/month", null,
                LocalDate.now(), LocalDate.now().minusDays(1), LocalDate.now().plusMonths(6)))
                .isInstanceOf(InvalidPlanningAssumptionException.class);
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

        assertThatThrownBy(() -> service.listAssumptions(householdId, false, null))
                .isInstanceOf(HouseholdNotFoundException.class);
    }

    @Test
    void listsFullHistoryWhenActiveOnlyIsFalse() {
        UUID householdId = UUID.randomUUID();
        when(householdRepository.existsById(householdId)).thenReturn(true);
        when(planningAssumptionRepository.findByHousehold_IdOrderByNameAscCreatedAtAscIdAsc(householdId))
                .thenReturn(List.of());

        assertThat(service.listAssumptions(householdId, false, null)).isEmpty();
    }

    @Test
    void rejectsActiveOnlyListingWithoutExplicitAsOf() {
        UUID householdId = UUID.randomUUID();
        when(householdRepository.existsById(householdId)).thenReturn(true);

        assertThatThrownBy(() -> service.listAssumptions(householdId, true, null))
                .isInstanceOf(InvalidPlanningAssumptionException.class);
    }

    @Test
    void listsActiveAsOfExplicitDateWhenActiveOnlyIsTrue() {
        UUID householdId = UUID.randomUUID();
        LocalDate asOf = LocalDate.now();
        when(householdRepository.existsById(householdId)).thenReturn(true);
        when(planningAssumptionRepository.findActiveAsOf(householdId, asOf)).thenReturn(List.of());

        assertThat(service.listAssumptions(householdId, true, asOf)).isEmpty();
    }

    @Test
    void throwsNotFoundWhenSupersedingForUnknownHousehold() {
        UUID householdId = UUID.randomUUID();
        when(householdRepository.existsById(householdId)).thenReturn(false);

        assertThatThrownBy(() -> service.supersedeAssumption(
                householdId, UUID.randomUUID(), "Future monthly income", "160000", "PHP/month", null,
                LocalDate.now(), null, LocalDate.now().plusMonths(6)))
                .isInstanceOf(HouseholdNotFoundException.class);
    }

    @Test
    void throwsNotFoundWhenSupersedingAssumptionFromAnotherHousehold() {
        UUID householdId = UUID.randomUUID();
        UUID assumptionId = UUID.randomUUID();
        when(householdRepository.existsById(householdId)).thenReturn(true);
        when(planningAssumptionRepository.findByIdAndHousehold_Id(assumptionId, householdId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.supersedeAssumption(
                householdId, assumptionId, "Future monthly income", "160000", "PHP/month", null,
                LocalDate.now(), null, LocalDate.now().plusMonths(6)))
                .isInstanceOf(PlanningAssumptionNotFoundException.class);
    }

    @Test
    void rejectsSupersedingAnAlreadySupersededAssumption() {
        UUID householdId = UUID.randomUUID();
        PlanningAssumption prior = mock(PlanningAssumption.class);
        when(prior.getName()).thenReturn("Future monthly income");
        when(prior.getSupersededById()).thenReturn(UUID.randomUUID());
        when(householdRepository.existsById(householdId)).thenReturn(true);
        when(planningAssumptionRepository.findByIdAndHousehold_Id(any(), any())).thenReturn(Optional.of(prior));

        assertThatThrownBy(() -> service.supersedeAssumption(
                householdId, UUID.randomUUID(), "Future monthly income", "160000", "PHP/month", null,
                LocalDate.now(), null, LocalDate.now().plusMonths(6)))
                .isInstanceOf(AssumptionAlreadySupersededException.class);
    }

    @Test
    void rejectsSupersedingWhenConcurrentRequestWinsTheConditionalUpdate() {
        UUID householdId = UUID.randomUUID();
        Household household = new Household("Ralph Household", "PHP");
        PlanningAssumption prior = new PlanningAssumption(
                household, "Future monthly income", "150000", "PHP/month", null,
                LocalDate.now(), null, LocalDate.now().plusMonths(6));
        when(householdRepository.existsById(householdId)).thenReturn(true);
        when(planningAssumptionRepository.findByIdAndHousehold_Id(any(), any())).thenReturn(Optional.of(prior));
        when(planningAssumptionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        // Simulates a concurrent transaction having already claimed the link between our
        // in-memory pre-check (which still sees the prior version as unsuperseded) and our
        // own conditional update: the database-level guard is what must catch this race.
        when(planningAssumptionRepository.linkSupersessionIfNotAlreadySuperseded(any(), any())).thenReturn(0);

        assertThatThrownBy(() -> service.supersedeAssumption(
                householdId, UUID.randomUUID(), "Future monthly income", "160000", "PHP/month", null,
                LocalDate.now(), null, LocalDate.now().plusMonths(6)))
                .isInstanceOf(AssumptionAlreadySupersededException.class);
    }

    @Test
    void rejectsSupersessionWithMismatchedName() {
        UUID householdId = UUID.randomUUID();
        Household household = new Household("Ralph Household", "PHP");
        PlanningAssumption prior = new PlanningAssumption(
                household, "Future monthly income", "150000", "PHP/month", null,
                LocalDate.now(), null, LocalDate.now().plusMonths(6));
        when(householdRepository.existsById(householdId)).thenReturn(true);
        when(planningAssumptionRepository.findByIdAndHousehold_Id(any(), any())).thenReturn(Optional.of(prior));

        assertThatThrownBy(() -> service.supersedeAssumption(
                householdId, UUID.randomUUID(), "Expected investment return", "160000", "PHP/month", null,
                LocalDate.now(), null, LocalDate.now().plusMonths(6)))
                .isInstanceOf(InvalidPlanningAssumptionException.class);
    }

    @Test
    void rejectsSupersessionWithEffectiveUntilBeforeEffectiveFrom() {
        UUID householdId = UUID.randomUUID();
        Household household = new Household("Ralph Household", "PHP");
        PlanningAssumption prior = new PlanningAssumption(
                household, "Future monthly income", "150000", "PHP/month", null,
                LocalDate.now(), null, LocalDate.now().plusMonths(6));
        when(householdRepository.existsById(householdId)).thenReturn(true);
        when(planningAssumptionRepository.findByIdAndHousehold_Id(any(), any())).thenReturn(Optional.of(prior));

        assertThatThrownBy(() -> service.supersedeAssumption(
                householdId, UUID.randomUUID(), "Future monthly income", "160000", "PHP/month", null,
                LocalDate.now(), LocalDate.now().minusDays(1), LocalDate.now().plusMonths(6)))
                .isInstanceOf(InvalidPlanningAssumptionException.class);
    }

    @Test
    void supersedingLinksPriorVersionToReplacementWhenConditionalUpdateSucceeds() {
        UUID householdId = UUID.randomUUID();
        Household household = new Household("Ralph Household", "PHP");
        PlanningAssumption prior = new PlanningAssumption(
                household, "Future monthly income", "150000", "PHP/month", null,
                LocalDate.now(), null, LocalDate.now().plusMonths(6));
        when(householdRepository.existsById(householdId)).thenReturn(true);
        when(planningAssumptionRepository.findByIdAndHousehold_Id(any(), any())).thenReturn(Optional.of(prior));
        when(planningAssumptionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(planningAssumptionRepository.linkSupersessionIfNotAlreadySuperseded(any(), any())).thenReturn(1);

        PlanningAssumption replacement = service.supersedeAssumption(
                householdId, UUID.randomUUID(), "Future monthly income", "160000", "PHP/month", "Raise confirmed",
                LocalDate.now(), null, LocalDate.now().plusMonths(6));

        assertThat(replacement.getValue()).isEqualTo("160000");
        assertThat(replacement.getSupersededById()).isNull();
        verify(planningAssumptionRepository).linkSupersessionIfNotAlreadySuperseded(any(), any());
    }
}
