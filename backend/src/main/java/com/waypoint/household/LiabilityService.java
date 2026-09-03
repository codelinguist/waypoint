package com.waypoint.household;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LiabilityService {

    private final HouseholdRepository householdRepository;
    private final LiabilityRepository liabilityRepository;

    public LiabilityService(HouseholdRepository householdRepository, LiabilityRepository liabilityRepository) {
        this.householdRepository = householdRepository;
        this.liabilityRepository = liabilityRepository;
    }

    public Liability createLiability(
            UUID householdId,
            String name,
            LiabilityType liabilityType,
            BigDecimal outstandingBalance,
            String currency,
            LocalDate balanceAsOf
    ) {
        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new HouseholdNotFoundException(householdId));
        Liability liability = new Liability(
                household,
                name.trim(),
                liabilityType,
                outstandingBalance,
                currency.trim().toUpperCase(),
                balanceAsOf
        );
        return liabilityRepository.save(liability);
    }

    @Transactional(readOnly = true)
    public Liability getLiability(UUID householdId, UUID liabilityId) {
        if (!householdRepository.existsById(householdId)) {
            throw new HouseholdNotFoundException(householdId);
        }
        return liabilityRepository.findByIdAndHousehold_Id(liabilityId, householdId)
                .orElseThrow(() -> new LiabilityNotFoundException(liabilityId));
    }

    @Transactional(readOnly = true)
    public List<Liability> listLiabilities(UUID householdId) {
        if (!householdRepository.existsById(householdId)) {
            throw new HouseholdNotFoundException(householdId);
        }
        return liabilityRepository.findByHousehold_IdOrderByCreatedAtAscIdAsc(householdId);
    }
}
