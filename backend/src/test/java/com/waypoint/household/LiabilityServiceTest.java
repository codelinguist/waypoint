package com.waypoint.household;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LiabilityServiceTest {

    private final HouseholdRepository householdRepository = mock(HouseholdRepository.class);
    private final LiabilityRepository liabilityRepository = mock(LiabilityRepository.class);
    private final LiabilityBalanceHistoryRepository liabilityBalanceHistoryRepository =
            mock(LiabilityBalanceHistoryRepository.class);
    private final LiabilityService liabilityService =
            new LiabilityService(householdRepository, liabilityRepository, liabilityBalanceHistoryRepository);

    @Test
    void normalizesNameAndCurrencyOnCreate() {
        UUID householdId = UUID.randomUUID();
        Household household = new Household("Ralph Household", "PHP");
        when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));
        when(liabilityRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Liability created = liabilityService.createLiability(
                householdId,
                "  Credit Card  ",
                LiabilityType.CREDIT_CARD,
                new BigDecimal("500.00"),
                "php",
                LocalDate.now()
        );

        assertThat(created.getName()).isEqualTo("Credit Card");
        assertThat(created.getCurrency()).isEqualTo("PHP");
        assertThat(created.getSourceType()).isEqualTo(SourceType.MANUAL_ENTRY);
        assertThat(created.getHousehold()).isSameAs(household);
    }

    @Test
    void throwsNotFoundWhenCreatingLiabilityForUnknownHousehold() {
        UUID householdId = UUID.randomUUID();
        when(householdRepository.findById(householdId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> liabilityService.createLiability(
                householdId, "Loan", LiabilityType.PERSONAL_LOAN, BigDecimal.TEN, "PHP", LocalDate.now()
        )).isInstanceOf(HouseholdNotFoundException.class);
    }

    @Test
    void returnsLiabilityScopedToHousehold() {
        UUID householdId = UUID.randomUUID();
        UUID liabilityId = UUID.randomUUID();
        Household household = new Household("Ralph Household", "PHP");
        Liability liability = new Liability(household, "Loan", LiabilityType.PERSONAL_LOAN, BigDecimal.TEN, "PHP",
                LocalDate.now());
        when(householdRepository.existsById(householdId)).thenReturn(true);
        when(liabilityRepository.findByIdAndHousehold_Id(liabilityId, householdId))
                .thenReturn(Optional.of(liability));

        assertThat(liabilityService.getLiability(householdId, liabilityId)).isSameAs(liability);
    }

    @Test
    void throwsNotFoundWhenLiabilityBelongsToAnotherHousehold() {
        UUID householdId = UUID.randomUUID();
        UUID liabilityId = UUID.randomUUID();
        when(householdRepository.existsById(householdId)).thenReturn(true);
        when(liabilityRepository.findByIdAndHousehold_Id(liabilityId, householdId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> liabilityService.getLiability(householdId, liabilityId))
                .isInstanceOf(LiabilityNotFoundException.class);
    }

    @Test
    void throwsNotFoundWhenListingLiabilitiesForUnknownHousehold() {
        UUID householdId = UUID.randomUUID();
        when(householdRepository.existsById(householdId)).thenReturn(false);

        assertThatThrownBy(() -> liabilityService.listLiabilities(householdId))
                .isInstanceOf(HouseholdNotFoundException.class);
    }

    @Test
    void listsLiabilitiesInCreationOrderForKnownHousehold() {
        UUID householdId = UUID.randomUUID();
        when(householdRepository.existsById(householdId)).thenReturn(true);
        when(liabilityRepository.findByHousehold_IdOrderByCreatedAtAscIdAsc(householdId)).thenReturn(List.of());

        assertThat(liabilityService.listLiabilities(householdId)).isEmpty();
    }

    @Test
    void recordsBalanceReplacementPreservingPreviousStateInHistory() {
        UUID householdId = UUID.randomUUID();
        UUID liabilityId = UUID.randomUUID();
        Household household = new Household("Ralph Household", "PHP");
        Liability before = new Liability(household, "Loan", LiabilityType.PERSONAL_LOAN,
                new BigDecimal("500.00"), "PHP", LocalDate.of(2026, 1, 1));
        Liability after = new Liability(household, "Loan", LiabilityType.PERSONAL_LOAN,
                new BigDecimal("300.00"), "PHP", LocalDate.of(2026, 2, 1));
        when(householdRepository.existsById(householdId)).thenReturn(true);
        when(liabilityRepository.findByIdAndHousehold_Id(liabilityId, householdId))
                .thenReturn(Optional.of(before), Optional.of(after));
        when(liabilityRepository.applyBalance(eq(liabilityId), eq(householdId), eq(new BigDecimal("300.00")),
                eq(LocalDate.of(2026, 2, 1)), any(Instant.class), eq(0L)))
                .thenReturn(1);
        when(liabilityBalanceHistoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LiabilityBalanceHistory history = liabilityService.recordBalance(
                householdId, liabilityId, new BigDecimal("300.00"), LocalDate.of(2026, 2, 1), "  Paid down  ", 0L
        );

        assertThat(history.getPreviousBalance()).isEqualByComparingTo("500.00");
        assertThat(history.getPreviousBalanceAsOf()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(history.getNewBalance()).isEqualByComparingTo("300.00");
        assertThat(history.getNewBalanceAsOf()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(history.getReason()).isEqualTo("Paid down");
        assertThat(history.getLiability()).isSameAs(after);
    }

    @Test
    void rejectsBalanceReplacementWhenNoRowMatchesTheExpectedRevision() {
        UUID householdId = UUID.randomUUID();
        UUID liabilityId = UUID.randomUUID();
        Household household = new Household("Ralph Household", "PHP");
        Liability liability = new Liability(household, "Loan", LiabilityType.PERSONAL_LOAN,
                new BigDecimal("500.00"), "PHP", LocalDate.of(2026, 1, 1));
        when(householdRepository.existsById(householdId)).thenReturn(true);
        when(liabilityRepository.findByIdAndHousehold_Id(liabilityId, householdId))
                .thenReturn(Optional.of(liability));
        when(liabilityRepository.applyBalance(eq(liabilityId), eq(householdId), eq(new BigDecimal("300.00")),
                eq(LocalDate.of(2026, 2, 1)), any(Instant.class), eq(5L)))
                .thenReturn(0);

        assertThatThrownBy(() -> liabilityService.recordBalance(
                householdId, liabilityId, new BigDecimal("300.00"), LocalDate.of(2026, 2, 1), "Paid down", 5L
        )).isInstanceOf(StaleLiabilityRevisionException.class);
        verify(liabilityBalanceHistoryRepository, never()).save(any());
    }

    @Test
    void throwsNotFoundWhenRecordingBalanceForUnknownHousehold() {
        UUID householdId = UUID.randomUUID();
        UUID liabilityId = UUID.randomUUID();
        when(householdRepository.existsById(householdId)).thenReturn(false);

        assertThatThrownBy(() -> liabilityService.recordBalance(
                householdId, liabilityId, BigDecimal.TEN, LocalDate.now(), "Reason", 0L
        )).isInstanceOf(HouseholdNotFoundException.class);
    }

    @Test
    void throwsNotFoundWhenRecordingBalanceForLiabilityInAnotherHousehold() {
        UUID householdId = UUID.randomUUID();
        UUID liabilityId = UUID.randomUUID();
        when(householdRepository.existsById(householdId)).thenReturn(true);
        when(liabilityRepository.findByIdAndHousehold_Id(liabilityId, householdId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> liabilityService.recordBalance(
                householdId, liabilityId, BigDecimal.TEN, LocalDate.now(), "Reason", 0L
        )).isInstanceOf(LiabilityNotFoundException.class);
    }

    @Test
    void listsBalanceHistoryInRevisionOrderForKnownLiability() {
        UUID householdId = UUID.randomUUID();
        UUID liabilityId = UUID.randomUUID();
        when(householdRepository.existsById(householdId)).thenReturn(true);
        when(liabilityRepository.existsByIdAndHousehold_Id(liabilityId, householdId)).thenReturn(true);
        when(liabilityBalanceHistoryRepository.findByLiability_IdOrderByRevisionAsc(liabilityId))
                .thenReturn(List.of());

        assertThat(liabilityService.listBalanceHistory(householdId, liabilityId)).isEmpty();
    }

    @Test
    void throwsNotFoundWhenListingBalanceHistoryForUnknownLiability() {
        UUID householdId = UUID.randomUUID();
        UUID liabilityId = UUID.randomUUID();
        when(householdRepository.existsById(householdId)).thenReturn(true);
        when(liabilityRepository.existsByIdAndHousehold_Id(liabilityId, householdId)).thenReturn(false);

        assertThatThrownBy(() -> liabilityService.listBalanceHistory(householdId, liabilityId))
                .isInstanceOf(LiabilityNotFoundException.class);
    }
}
