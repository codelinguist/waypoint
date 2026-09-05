package com.waypoint.household;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Compares one persisted {@link FinancialSnapshot}'s actual totals with
 * caller-supplied planned totals. The plan is a disposable analysis input:
 * nothing here is read from or written to a persistent planning entity, and
 * the source snapshot is never mutated.
 */
@Service
@Transactional(readOnly = true)
public class PlanVersusActualService {

    private final FinancialSnapshotService financialSnapshotService;

    public PlanVersusActualService(FinancialSnapshotService financialSnapshotService) {
        this.financialSnapshotService = financialSnapshotService;
    }

    public PlanVersusActualAnalysis analyze(
            UUID householdId, UUID snapshotId, List<PlannedCurrencyTotals> plannedMeasures
    ) {
        List<PlannedCurrencyTotals> normalizedPlan = normalize(plannedMeasures);
        validatePlan(normalizedPlan);

        FinancialSnapshotDetail detail = financialSnapshotService.getSnapshot(householdId, snapshotId);
        Map<String, CurrencyTotals> actualByCurrency = new TreeMap<>();
        detail.totalsByCurrency().forEach(totals -> actualByCurrency.put(totals.currency(), totals));

        List<CurrencyPlanVersusActual> results = new ArrayList<>();
        for (PlannedCurrencyTotals planned : normalizedPlan) {
            CurrencyTotals actual = actualByCurrency.getOrDefault(planned.currency(), zeroTotals(planned.currency()));
            results.add(new CurrencyPlanVersusActual(
                    planned.currency(),
                    PlanVersusActualVariance.of(planned.assetTotal(), actual.assetTotal()),
                    PlanVersusActualVariance.of(planned.liabilityTotal(), actual.liabilityTotal()),
                    PlanVersusActualVariance.of(planned.netWorth(), actual.netWorth())
            ));
        }
        return new PlanVersusActualAnalysis(detail.snapshot(), results);
    }

    private List<PlannedCurrencyTotals> normalize(List<PlannedCurrencyTotals> plannedMeasures) {
        return plannedMeasures.stream()
                .map(planned -> new PlannedCurrencyTotals(
                        planned.currency().trim().toUpperCase(),
                        planned.assetTotal(),
                        planned.liabilityTotal(),
                        planned.netWorth()))
                .toList();
    }

    private void validatePlan(List<PlannedCurrencyTotals> plannedMeasures) {
        Set<String> seenCurrencies = new HashSet<>();
        for (PlannedCurrencyTotals planned : plannedMeasures) {
            if (!seenCurrencies.add(planned.currency())) {
                throw new InvalidPlanException("Duplicate planned currency: " + planned.currency());
            }
            if (planned.assetTotal().signum() < 0) {
                throw new InvalidPlanException("Planned assetTotal must not be negative for " + planned.currency());
            }
            if (planned.liabilityTotal().signum() < 0) {
                throw new InvalidPlanException(
                        "Planned liabilityTotal must not be negative for " + planned.currency());
            }
            BigDecimal expectedNetWorth = planned.assetTotal().subtract(planned.liabilityTotal());
            if (planned.netWorth().compareTo(expectedNetWorth) != 0) {
                throw new InvalidPlanException(
                        "Planned netWorth must equal assetTotal minus liabilityTotal for " + planned.currency());
            }
        }
    }

    private CurrencyTotals zeroTotals(String currency) {
        return new CurrencyTotals(currency, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
