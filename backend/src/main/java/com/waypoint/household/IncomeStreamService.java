package com.waypoint.household;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class IncomeStreamService {

    private final HouseholdRepository householdRepository;
    private final IncomeStreamRepository incomeStreamRepository;

    public IncomeStreamService(HouseholdRepository householdRepository, IncomeStreamRepository incomeStreamRepository) {
        this.householdRepository = householdRepository;
        this.incomeStreamRepository = incomeStreamRepository;
    }

    public IncomeStream createIncomeStream(
            UUID householdId,
            String name,
            IncomeType incomeType,
            BigDecimal amount,
            Frequency frequency,
            String currency,
            CompensationClassification compensationClassification,
            IncomeCertainty certainty,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new HouseholdNotFoundException(householdId));
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new InvalidScheduleException("endDate must not precede startDate");
        }
        IncomeStream incomeStream = new IncomeStream(
                household,
                name.trim(),
                incomeType,
                amount,
                frequency,
                currency.trim().toUpperCase(),
                compensationClassification,
                certainty,
                startDate,
                endDate
        );
        return incomeStreamRepository.save(incomeStream);
    }

    @Transactional(readOnly = true)
    public IncomeStream getIncomeStream(UUID householdId, UUID incomeStreamId) {
        if (!householdRepository.existsById(householdId)) {
            throw new HouseholdNotFoundException(householdId);
        }
        return incomeStreamRepository.findByIdAndHousehold_Id(incomeStreamId, householdId)
                .orElseThrow(() -> new IncomeStreamNotFoundException(incomeStreamId));
    }

    @Transactional(readOnly = true)
    public List<IncomeStream> listIncomeStreams(UUID householdId) {
        if (!householdRepository.existsById(householdId)) {
            throw new HouseholdNotFoundException(householdId);
        }
        return incomeStreamRepository.findByHousehold_IdOrderByCreatedAtAscIdAsc(householdId);
    }
}
