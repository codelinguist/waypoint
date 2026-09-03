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

class IncomeStreamServiceTest {

    private final HouseholdRepository householdRepository = mock(HouseholdRepository.class);
    private final IncomeStreamRepository incomeStreamRepository = mock(IncomeStreamRepository.class);
    private final IncomeStreamService incomeStreamService =
            new IncomeStreamService(householdRepository, incomeStreamRepository);

    @Test
    void normalizesNameAndCurrencyOnCreate() {
        UUID householdId = UUID.randomUUID();
        Household household = new Household("Ralph Household", "PHP");
        when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));
        when(incomeStreamRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        IncomeStream created = incomeStreamService.createIncomeStream(
                householdId,
                "  October Salary  ",
                IncomeType.SALARY,
                new BigDecimal("50000.00"),
                Frequency.MONTHLY,
                "php",
                CompensationClassification.GROSS,
                IncomeCertainty.EXPECTED,
                LocalDate.now().plusMonths(1),
                null
        );

        assertThat(created.getName()).isEqualTo("October Salary");
        assertThat(created.getCurrency()).isEqualTo("PHP");
        assertThat(created.getSourceType()).isEqualTo(SourceType.MANUAL_ENTRY);
        assertThat(created.getHousehold()).isSameAs(household);
        assertThat(created.getCertainty()).isEqualTo(IncomeCertainty.EXPECTED);
        assertThat(created.getCompensationClassification()).isEqualTo(CompensationClassification.GROSS);
    }

    @Test
    void throwsNotFoundWhenCreatingIncomeStreamForUnknownHousehold() {
        UUID householdId = UUID.randomUUID();
        when(householdRepository.findById(householdId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> incomeStreamService.createIncomeStream(
                householdId, "Salary", IncomeType.SALARY, BigDecimal.TEN, Frequency.MONTHLY, "PHP",
                CompensationClassification.GROSS, IncomeCertainty.CONFIRMED, LocalDate.now(), null
        )).isInstanceOf(HouseholdNotFoundException.class);
    }

    @Test
    void rejectsEndDateBeforeStartDate() {
        UUID householdId = UUID.randomUUID();
        Household household = new Household("Ralph Household", "PHP");
        when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));

        assertThatThrownBy(() -> incomeStreamService.createIncomeStream(
                householdId, "Salary", IncomeType.SALARY, BigDecimal.TEN, Frequency.MONTHLY, "PHP",
                CompensationClassification.GROSS, IncomeCertainty.CONFIRMED,
                LocalDate.now(), LocalDate.now().minusDays(1)
        )).isInstanceOf(InvalidScheduleException.class);
    }

    @Test
    void returnsIncomeStreamScopedToHousehold() {
        UUID householdId = UUID.randomUUID();
        UUID incomeStreamId = UUID.randomUUID();
        Household household = new Household("Ralph Household", "PHP");
        IncomeStream incomeStream = new IncomeStream(household, "Salary", IncomeType.SALARY, BigDecimal.TEN,
                Frequency.MONTHLY, "PHP", CompensationClassification.GROSS, IncomeCertainty.CONFIRMED,
                LocalDate.now(), null);
        when(householdRepository.existsById(householdId)).thenReturn(true);
        when(incomeStreamRepository.findByIdAndHousehold_Id(incomeStreamId, householdId))
                .thenReturn(Optional.of(incomeStream));

        assertThat(incomeStreamService.getIncomeStream(householdId, incomeStreamId)).isSameAs(incomeStream);
    }

    @Test
    void throwsNotFoundWhenIncomeStreamBelongsToAnotherHousehold() {
        UUID householdId = UUID.randomUUID();
        UUID incomeStreamId = UUID.randomUUID();
        when(householdRepository.existsById(householdId)).thenReturn(true);
        when(incomeStreamRepository.findByIdAndHousehold_Id(incomeStreamId, householdId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> incomeStreamService.getIncomeStream(householdId, incomeStreamId))
                .isInstanceOf(IncomeStreamNotFoundException.class);
    }

    @Test
    void throwsNotFoundWhenListingIncomeStreamsForUnknownHousehold() {
        UUID householdId = UUID.randomUUID();
        when(householdRepository.existsById(householdId)).thenReturn(false);

        assertThatThrownBy(() -> incomeStreamService.listIncomeStreams(householdId))
                .isInstanceOf(HouseholdNotFoundException.class);
    }

    @Test
    void listsIncomeStreamsInCreationOrderForKnownHousehold() {
        UUID householdId = UUID.randomUUID();
        when(householdRepository.existsById(householdId)).thenReturn(true);
        when(incomeStreamRepository.findByHousehold_IdOrderByCreatedAtAscIdAsc(householdId)).thenReturn(List.of());

        assertThat(incomeStreamService.listIncomeStreams(householdId)).isEmpty();
    }
}
