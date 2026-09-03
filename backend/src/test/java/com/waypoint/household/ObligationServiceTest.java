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

class ObligationServiceTest {

    private final HouseholdRepository householdRepository = mock(HouseholdRepository.class);
    private final ObligationRepository obligationRepository = mock(ObligationRepository.class);
    private final ObligationService obligationService =
            new ObligationService(householdRepository, obligationRepository);

    @Test
    void normalizesNameAndCurrencyOnCreate() {
        UUID householdId = UUID.randomUUID();
        Household household = new Household("Ralph Household", "PHP");
        when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));
        when(obligationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Obligation created = obligationService.createObligation(
                householdId,
                "  Mortgage  ",
                ObligationType.MORTGAGE,
                new BigDecimal("25000.00"),
                Frequency.MONTHLY,
                "php",
                LocalDate.now(),
                null
        );

        assertThat(created.getName()).isEqualTo("Mortgage");
        assertThat(created.getCurrency()).isEqualTo("PHP");
        assertThat(created.getSourceType()).isEqualTo(SourceType.MANUAL_ENTRY);
        assertThat(created.getHousehold()).isSameAs(household);
    }

    @Test
    void throwsNotFoundWhenCreatingObligationForUnknownHousehold() {
        UUID householdId = UUID.randomUUID();
        when(householdRepository.findById(householdId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> obligationService.createObligation(
                householdId, "Mortgage", ObligationType.MORTGAGE, BigDecimal.TEN, Frequency.MONTHLY, "PHP",
                LocalDate.now(), null
        )).isInstanceOf(HouseholdNotFoundException.class);
    }

    @Test
    void rejectsEndDateBeforeStartDate() {
        UUID householdId = UUID.randomUUID();
        Household household = new Household("Ralph Household", "PHP");
        when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));

        assertThatThrownBy(() -> obligationService.createObligation(
                householdId, "Mortgage", ObligationType.MORTGAGE, BigDecimal.TEN, Frequency.MONTHLY, "PHP",
                LocalDate.now(), LocalDate.now().minusDays(1)
        )).isInstanceOf(InvalidScheduleException.class);
    }

    @Test
    void returnsObligationScopedToHousehold() {
        UUID householdId = UUID.randomUUID();
        UUID obligationId = UUID.randomUUID();
        Household household = new Household("Ralph Household", "PHP");
        Obligation obligation = new Obligation(household, "Mortgage", ObligationType.MORTGAGE, BigDecimal.TEN,
                Frequency.MONTHLY, "PHP", LocalDate.now(), null);
        when(householdRepository.existsById(householdId)).thenReturn(true);
        when(obligationRepository.findByIdAndHousehold_Id(obligationId, householdId))
                .thenReturn(Optional.of(obligation));

        assertThat(obligationService.getObligation(householdId, obligationId)).isSameAs(obligation);
    }

    @Test
    void throwsNotFoundWhenObligationBelongsToAnotherHousehold() {
        UUID householdId = UUID.randomUUID();
        UUID obligationId = UUID.randomUUID();
        when(householdRepository.existsById(householdId)).thenReturn(true);
        when(obligationRepository.findByIdAndHousehold_Id(obligationId, householdId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> obligationService.getObligation(householdId, obligationId))
                .isInstanceOf(ObligationNotFoundException.class);
    }

    @Test
    void throwsNotFoundWhenListingObligationsForUnknownHousehold() {
        UUID householdId = UUID.randomUUID();
        when(householdRepository.existsById(householdId)).thenReturn(false);

        assertThatThrownBy(() -> obligationService.listObligations(householdId))
                .isInstanceOf(HouseholdNotFoundException.class);
    }

    @Test
    void listsObligationsInCreationOrderForKnownHousehold() {
        UUID householdId = UUID.randomUUID();
        when(householdRepository.existsById(householdId)).thenReturn(true);
        when(obligationRepository.findByHousehold_IdOrderByCreatedAtAscIdAsc(householdId)).thenReturn(List.of());

        assertThat(obligationService.listObligations(householdId)).isEmpty();
    }
}
