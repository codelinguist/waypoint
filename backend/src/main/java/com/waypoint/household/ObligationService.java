package com.waypoint.household;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ObligationService {

    private final HouseholdRepository householdRepository;
    private final ObligationRepository obligationRepository;

    public ObligationService(HouseholdRepository householdRepository, ObligationRepository obligationRepository) {
        this.householdRepository = householdRepository;
        this.obligationRepository = obligationRepository;
    }

    public Obligation createObligation(
            UUID householdId,
            String name,
            ObligationType obligationType,
            BigDecimal amount,
            Frequency frequency,
            String currency,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new HouseholdNotFoundException(householdId));
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new InvalidScheduleException("endDate must not precede startDate");
        }
        Obligation obligation = new Obligation(
                household,
                name.trim(),
                obligationType,
                amount,
                frequency,
                currency.trim().toUpperCase(),
                startDate,
                endDate
        );
        return obligationRepository.save(obligation);
    }

    @Transactional(readOnly = true)
    public Obligation getObligation(UUID householdId, UUID obligationId) {
        if (!householdRepository.existsById(householdId)) {
            throw new HouseholdNotFoundException(householdId);
        }
        return obligationRepository.findByIdAndHousehold_Id(obligationId, householdId)
                .orElseThrow(() -> new ObligationNotFoundException(obligationId));
    }

    @Transactional(readOnly = true)
    public List<Obligation> listObligations(UUID householdId) {
        if (!householdRepository.existsById(householdId)) {
            throw new HouseholdNotFoundException(householdId);
        }
        return obligationRepository.findByHousehold_IdOrderByCreatedAtAscIdAsc(householdId);
    }
}
