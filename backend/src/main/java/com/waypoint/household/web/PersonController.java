package com.waypoint.household.web;

import com.waypoint.household.Person;
import com.waypoint.household.PersonService;
import com.waypoint.household.web.dto.CreatePersonRequest;
import com.waypoint.household.web.dto.PersonResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/households/{householdId}/people")
public class PersonController {

    private final PersonService personService;

    public PersonController(PersonService personService) {
        this.personService = personService;
    }

    @PostMapping
    public ResponseEntity<PersonResponse> addPerson(
            @PathVariable UUID householdId,
            @Valid @RequestBody CreatePersonRequest request
    ) {
        Person person = personService.addPerson(householdId, request.name(), request.role());
        return ResponseEntity.status(HttpStatus.CREATED).body(PersonResponse.from(person));
    }

    @GetMapping
    public ResponseEntity<List<PersonResponse>> listPeople(@PathVariable UUID householdId) {
        List<PersonResponse> people = personService.listPeople(householdId).stream()
                .map(PersonResponse::from)
                .toList();
        return ResponseEntity.ok(people);
    }
}
