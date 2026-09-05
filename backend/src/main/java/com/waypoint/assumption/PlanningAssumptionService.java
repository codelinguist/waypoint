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
        validateDateOrdering(effectiveFrom, effectiveUntil);
        PlanningAssumption assumption = new PlanningAssumption(
                household,
                name.trim(),
                value.trim(),
                valueType.trim(),
                normalizeNotes(notes),
                effectiveFrom,
                effectiveUntil,
                reviewDate
        );
        return planningAssumptionRepository.save(assumption);
    }

    @Transactional(readOnly = true)
    public PlanningAssumption getAssumption(UUID householdId, UUID assumptionId) {
        ensureHouseholdExists(householdId);
        return planningAssumptionRepository.findByIdAndHousehold_Id(assumptionId, householdId)
                .orElseThrow(() -> new PlanningAssumptionNotFoundException(assumptionId));
    }

    @Transactional(readOnly = true)
    public List<PlanningAssumption> listAssumptions(UUID householdId) {
        ensureHouseholdExists(householdId);
        return planningAssumptionRepository.findByHousehold_IdOrderByNameAscEffectiveFromAscCreatedAtAscIdAsc(householdId);
    }

    @Transactional(readOnly = true)
    public List<PlanningAssumption> listActiveAssumptions(UUID householdId, LocalDate asOf) {
        ensureHouseholdExists(householdId);
        return planningAssumptionRepository.findByHousehold_IdOrderByNameAscEffectiveFromAscCreatedAtAscIdAsc(householdId)
                .stream()
                .filter(assumption -> assumption.isActiveAsOf(asOf))
                .toList();
    }

    /**
     * Atomically creates a replacement version and links the superseded
     * record to it. Rejects (and persists nothing for) a replacement that
     * targets an already-superseded version or names a different logical
     * assumption than the one it claims to replace.
     */
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
        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new HouseholdNotFoundException(householdId));
        PlanningAssumption prior = planningAssumptionRepository.findByIdAndHousehold_Id(assumptionId, householdId)
                .orElseThrow(() -> new PlanningAssumptionNotFoundException(assumptionId));
        if (prior.getSupersededBy() != null) {
            throw new InvalidPlanningAssumptionException(
                    "Planning assumption is already superseded: " + assumptionId);
        }
        String trimmedName = name.trim();
        if (!trimmedName.equals(prior.getName())) {
            throw new InvalidPlanningAssumptionException(
                    "Replacement must use the same assumption name as the version it supersedes");
        }
        validateDateOrdering(effectiveFrom, effectiveUntil);

        PlanningAssumption replacement = new PlanningAssumption(
                household,
                trimmedName,
                value.trim(),
                valueType.trim(),
                normalizeNotes(notes),
                effectiveFrom,
                effectiveUntil,
                reviewDate
        );
        PlanningAssumption savedReplacement = planningAssumptionRepository.save(replacement);
        prior.linkSupersededBy(savedReplacement);
        return savedReplacement;
    }

    private void ensureHouseholdExists(UUID householdId) {
        if (!householdRepository.existsById(householdId)) {
            throw new HouseholdNotFoundException(householdId);
        }
    }

    private static void validateDateOrdering(LocalDate effectiveFrom, LocalDate effectiveUntil) {
        if (effectiveUntil != null && effectiveUntil.isBefore(effectiveFrom)) {
            throw new InvalidPlanningAssumptionException("effectiveUntil must not precede effectiveFrom");
        }
    }

    private static String normalizeNotes(String notes) {
        if (notes == null) {
            return null;
        }
        String trimmed = notes.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
