package com.waypoint.position;

import com.waypoint.household.Asset;
import com.waypoint.household.CurrencyTotals;
import com.waypoint.household.Household;
import com.waypoint.household.HouseholdNotFoundException;
import com.waypoint.household.HouseholdRepository;
import com.waypoint.household.Liability;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only current-position query. The household lookup and both row reads run
 * inside one {@code REPEATABLE_READ} transaction, so every row a concurrent
 * write commits after this transaction starts is invisible to it: the returned
 * totals are always derived from exactly the rows also returned, never a mix of
 * before/after states. Nothing is written; retrieval time is metadata, not a
 * valuation date.
 */
@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class FinancialPositionService {

    private final HouseholdRepository householdRepository;
    private final PositionAssetRepository assetRepository;
    private final PositionLiabilityRepository liabilityRepository;

    public FinancialPositionService(
            HouseholdRepository householdRepository,
            PositionAssetRepository assetRepository,
            PositionLiabilityRepository liabilityRepository
    ) {
        this.householdRepository = householdRepository;
        this.assetRepository = assetRepository;
        this.liabilityRepository = liabilityRepository;
    }

    public FinancialPositionResult getCurrentFinancialPosition(UUID householdId) {
        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new HouseholdNotFoundException(householdId));
        List<Asset> assets = assetRepository.findByHousehold_IdOrderByIdAsc(householdId);
        List<Liability> liabilities = liabilityRepository.findByHousehold_IdOrderByIdAsc(householdId);
        List<CurrencyTotals> totals = CurrencyTotalsCalculator.compute(assets, liabilities);
        return new FinancialPositionResult(household, assets, liabilities, totals, Instant.now());
    }
}
