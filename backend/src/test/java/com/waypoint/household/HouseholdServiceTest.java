package com.waypoint.household;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class HouseholdServiceTest {

    private final HouseholdRepository householdRepository = mock(HouseholdRepository.class);
    private final HouseholdService householdService = new HouseholdService(householdRepository);

    @Test
    void normalizesNameAndCurrencyOnCreate() {
        when(householdRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Household created = householdService.createHousehold("  Ralph Household  ", "php");

        assertThat(created.getName()).isEqualTo("Ralph Household");
        assertThat(created.getBaseCurrency()).isEqualTo("PHP");
    }

    @Test
    void returnsHouseholdWhenFound() {
        Household household = new Household("Ralph Household", "PHP");
        UUID id = UUID.randomUUID();
        when(householdRepository.findById(id)).thenReturn(Optional.of(household));

        assertThat(householdService.getHousehold(id)).isSameAs(household);
    }

    @Test
    void throwsNotFoundForUnknownHousehold() {
        UUID id = UUID.randomUUID();
        when(householdRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> householdService.getHousehold(id))
                .isInstanceOf(HouseholdNotFoundException.class);
    }
}
