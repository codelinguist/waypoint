package com.waypoint.household.web;

import com.waypoint.household.Household;
import com.waypoint.household.HouseholdService;
import com.waypoint.household.web.dto.CreateHouseholdRequest;
import com.waypoint.household.web.dto.HouseholdResponse;
import jakarta.validation.Valid;
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
@RequestMapping("/api/households")
public class HouseholdController {

    private final HouseholdService householdService;

    public HouseholdController(HouseholdService householdService) {
        this.householdService = householdService;
    }

    @PostMapping
    public ResponseEntity<HouseholdResponse> createHousehold(@Valid @RequestBody CreateHouseholdRequest request) {
        Household household = householdService.createHousehold(request.name(), request.baseCurrency());
        return ResponseEntity.status(HttpStatus.CREATED).body(HouseholdResponse.from(household));
    }

    @GetMapping("/{householdId}")
    public ResponseEntity<HouseholdResponse> getHousehold(@PathVariable UUID householdId) {
        Household household = householdService.getHousehold(householdId);
        return ResponseEntity.ok(HouseholdResponse.from(household));
    }
}
