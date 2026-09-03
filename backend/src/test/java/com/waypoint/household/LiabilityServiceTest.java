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

class LiabilityServiceTest {

    private final HouseholdRepository householdRepository = mock(HouseholdRepository.class);
    private final LiabilityRepository liabilityRepository = mock(LiabilityRepository.class);
    private final LiabilityService liabilityService = new LiabilityService(householdRepository, liabilityRepository);

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
}
