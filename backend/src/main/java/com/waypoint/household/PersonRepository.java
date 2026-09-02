package com.waypoint.household;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonRepository extends JpaRepository<Person, UUID> {

    List<Person> findByHousehold_IdOrderByCreatedAtAscIdAsc(UUID householdId);
}
