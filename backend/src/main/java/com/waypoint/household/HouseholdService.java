package com.waypoint.household;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class HouseholdService {

    private final HouseholdRepository householdRepository;

    public HouseholdService(HouseholdRepository householdRepository) {
        this.householdRepository = householdRepository;
    }

    public Household createHousehold(String name, String baseCurrency) {
        Household household = new Household(name.trim(), baseCurrency.trim().toUpperCase());
        return householdRepository.save(household);
    }

    @Transactional(readOnly = true)
    public Household getHousehold(UUID householdId) {
        return householdRepository.findById(householdId)
                .orElseThrow(() -> new HouseholdNotFoundException(householdId));
    }
}
