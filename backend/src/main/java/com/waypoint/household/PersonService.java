package com.waypoint.household;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PersonService {

    private final HouseholdRepository householdRepository;
    private final PersonRepository personRepository;

    public PersonService(HouseholdRepository householdRepository, PersonRepository personRepository) {
        this.householdRepository = householdRepository;
        this.personRepository = personRepository;
    }

    public Person addPerson(UUID householdId, String name, String role) {
        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new HouseholdNotFoundException(householdId));
        Person person = new Person(household, name.trim(), role.trim());
        return personRepository.save(person);
    }

    @Transactional(readOnly = true)
    public List<Person> listPeople(UUID householdId) {
        if (!householdRepository.existsById(householdId)) {
            throw new HouseholdNotFoundException(householdId);
        }
        return personRepository.findByHousehold_IdOrderByCreatedAtAscIdAsc(householdId);
    }
}
