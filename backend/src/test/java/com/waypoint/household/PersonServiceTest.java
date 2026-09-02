package com.waypoint.household;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PersonServiceTest {

    private final HouseholdRepository householdRepository = mock(HouseholdRepository.class);
    private final PersonRepository personRepository = mock(PersonRepository.class);
    private final PersonService personService = new PersonService(householdRepository, personRepository);

    @Test
    void normalizesNameAndRoleOnAdd() {
        UUID householdId = UUID.randomUUID();
        Household household = new Household("Ralph Household", "PHP");
        when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));
        when(personRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Person created = personService.addPerson(householdId, "  Ralph  ", "  Parent  ");

        assertThat(created.getName()).isEqualTo("Ralph");
        assertThat(created.getRole()).isEqualTo("Parent");
        assertThat(created.getHousehold()).isSameAs(household);
    }

    @Test
    void throwsNotFoundWhenAddingPersonToUnknownHousehold() {
        UUID householdId = UUID.randomUUID();
        when(householdRepository.findById(householdId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> personService.addPerson(householdId, "Ralph", "Parent"))
                .isInstanceOf(HouseholdNotFoundException.class);
    }

    @Test
    void throwsNotFoundWhenListingPeopleForUnknownHousehold() {
        UUID householdId = UUID.randomUUID();
        when(householdRepository.existsById(householdId)).thenReturn(false);

        assertThatThrownBy(() -> personService.listPeople(householdId))
                .isInstanceOf(HouseholdNotFoundException.class);
    }

    @Test
    void listsPeopleInCreationOrderForKnownHousehold() {
        UUID householdId = UUID.randomUUID();
        when(householdRepository.existsById(householdId)).thenReturn(true);
        when(personRepository.findByHousehold_IdOrderByCreatedAtAscIdAsc(householdId))
                .thenReturn(List.of());

        assertThat(personService.listPeople(householdId)).isEmpty();
    }
}
