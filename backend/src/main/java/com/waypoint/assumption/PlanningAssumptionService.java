package com.waypoint.assumption;

import com.waypoint.household.Household;
import com.waypoint.household.HouseholdNotFoundException;
import com.waypoint.household.HouseholdRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PlanningAssumptionService {

    private final HouseholdRepository householdRepository;
    private final PlanningAssumptionRepository planningAssumptionRepository;

    public PlanningAssumptionService(
            HouseholdRepository householdRepository,
            PlanningAssumptionRepository planningAssumptionRepository
    ) {
        this.householdRepository = householdRepository;
        this.planningAssumptionRepository = planningAssumptionRepository;
    }

    public PlanningAssumption createAssumption(
            UUID householdId,
            String name,
            String value,
            String valueType,
            String notes,
            LocalDate effectiveFrom,
            LocalDate effectiveUntil,
            LocalDate reviewDate
    ) {
        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new HouseholdNotFoundException(householdId));
        validateEffectiveWindow(effectiveFrom, effectiveUntil);
        PlanningAssumption assumption = new PlanningAssumption(
                household,
                name.trim(),
                value.trim(),
                valueType.trim(),
                trimToNull(notes),
                effectiveFrom,
                effectiveUntil,
                reviewDate
        );
        return planningAssumptionRepository.save(assumption);
    }

    @Transactional(readOnly = true)
    public PlanningAssumption getAssumption(UUID householdId, UUID assumptionId) {
        requireHousehold(householdId);
        return planningAssumptionRepository.findByIdAndHousehold_Id(assumptionId, householdId)
                .orElseThrow(() -> new PlanningAssumptionNotFoundException(assumptionId));
    }

    @Transactional(readOnly = true)
    public List<PlanningAssumption> listAssumptions(UUID householdId, boolean activeOnly, LocalDate asOf) {
        requireHousehold(householdId);
        if (activeOnly) {
            if (asOf == null) {
                throw new InvalidPlanningAssumptionException("asOf is required when activeOnly=true");
            }
            return planningAssumptionRepository.findActiveAsOf(householdId, asOf);
        }
        return planningAssumptionRepository.findByHousehold_IdOrderByNameAscCreatedAtAscIdAsc(householdId);
    }

    /**
     * Every unsuperseded version for the household, regardless of temporal
     * window — unlike {@link #listAssumptions}'s {@code activeOnly} mode,
     * this deliberately includes not-yet-effective and already-expired
     * versions so a review report can surface both dimensions independently.
     */
    @Transactional(readOnly = true)
    public List<PlanningAssumption> listUnsupersededAssumptions(UUID householdId) {
        requireHousehold(householdId);
        return planningAssumptionRepository.findByHousehold_IdAndSupersededByIsNull(householdId);
    }

    public PlanningAssumption supersedeAssumption(
            UUID householdId,
            UUID assumptionId,
            String name,
            String value,
            String valueType,
            String notes,
            LocalDate effectiveFrom,
            LocalDate effectiveUntil,
            LocalDate reviewDate
    ) {
        requireHousehold(householdId);
        PlanningAssumption priorVersion = planningAssumptionRepository.findByIdAndHousehold_Id(assumptionId, householdId)
                .orElseThrow(() -> new PlanningAssumptionNotFoundException(assumptionId));
        if (priorVersion.getSupersededById() != null) {
            throw new AssumptionAlreadySupersededException(assumptionId);
        }
        if (!priorVersion.getName().equals(name.trim())) {
            throw new InvalidPlanningAssumptionException(
                    "Replacement must use the same assumption name: " + priorVersion.getName());
        }
        validateEffectiveWindow(effectiveFrom, effectiveUntil);

        PlanningAssumption replacement = new PlanningAssumption(
                priorVersion.getHousehold(),
                priorVersion.getName(),
                value.trim(),
                valueType.trim(),
                trimToNull(notes),
                effectiveFrom,
                effectiveUntil,
                reviewDate
        );
        planningAssumptionRepository.save(replacement);

        int linked = planningAssumptionRepository.linkSupersessionIfNotAlreadySuperseded(
                priorVersion.getId(), replacement.getId());
        if (linked == 0) {
            // A concurrent request won the race to supersede the same prior version between our
            // read above and this conditional update; roll back this transaction's replacement.
            throw new AssumptionAlreadySupersededException(assumptionId);
        }
        return replacement;
    }

    private void requireHousehold(UUID householdId) {
        if (!householdRepository.existsById(householdId)) {
            throw new HouseholdNotFoundException(householdId);
        }
    }

    private void validateEffectiveWindow(LocalDate effectiveFrom, LocalDate effectiveUntil) {
        if (effectiveUntil != null && effectiveUntil.isBefore(effectiveFrom)) {
            throw new InvalidPlanningAssumptionException("effectiveUntil must not precede effectiveFrom");
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
