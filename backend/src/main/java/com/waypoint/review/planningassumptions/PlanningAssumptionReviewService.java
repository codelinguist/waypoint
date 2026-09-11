package com.waypoint.review.planningassumptions;

import com.waypoint.assumption.PlanningAssumption;
import com.waypoint.assumption.PlanningAssumptionService;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Assembles the household's current unsuperseded planning-assumption source
 * rows through the existing {@link PlanningAssumptionService} — which
 * already enforces household existence — and hands them to the pure
 * {@link PlanningAssumptionReviewCalculator}. Adds no new household
 * validation and performs no write.
 */
@Service
public class PlanningAssumptionReviewService {

    private final PlanningAssumptionService planningAssumptionService;
    private final PlanningAssumptionReviewCalculator calculator;

    public PlanningAssumptionReviewService(
            PlanningAssumptionService planningAssumptionService, PlanningAssumptionReviewCalculator calculator
    ) {
        this.planningAssumptionService = planningAssumptionService;
        this.calculator = calculator;
    }

    public PlanningAssumptionReviewResult review(UUID householdId, LocalDate asOf) {
        List<PlanningAssumption> assumptions = planningAssumptionService.listUnsupersededAssumptions(householdId);

        List<PlanningAssumptionReviewSourceRecord> sourceRecords = assumptions.stream()
                .map(assumption -> new PlanningAssumptionReviewSourceRecord(
                        assumption.getId(),
                        assumption.getName(),
                        assumption.getReviewDate(),
                        assumption.getEffectiveFrom(),
                        assumption.getEffectiveUntil(),
                        assumption.getSourceType()))
                .toList();

        return calculator.review(householdId, asOf, sourceRecords);
    }
}
