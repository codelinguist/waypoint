package com.waypoint.household.web.dto;

import com.waypoint.household.Person;
import java.time.Instant;
import java.util.UUID;

public record PersonResponse(
        UUID id,
        UUID householdId,
        String name,
        String role,
        Instant createdAt,
        Instant updatedAt
) {

    public static PersonResponse from(Person person) {
        return new PersonResponse(
                person.getId(),
                person.getHousehold().getId(),
                person.getName(),
                person.getRole(),
                person.getCreatedAt(),
                person.getUpdatedAt()
        );
    }
}
