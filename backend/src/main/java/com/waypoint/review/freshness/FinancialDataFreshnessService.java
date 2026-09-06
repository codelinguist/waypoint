package com.waypoint.review.freshness;

import com.waypoint.household.Asset;
import com.waypoint.household.AssetService;
import com.waypoint.household.Liability;
import com.waypoint.household.LiabilityService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Assembles the current asset and liability source rows for a household
 * through the existing {@link AssetService} and {@link LiabilityService} —
 * which already enforce household existence and ownership — and hands them
 * to the pure {@link FinancialDataFreshnessCalculator}. Adds no new
 * household-validation logic and performs no write.
 */
@Service
public class FinancialDataFreshnessService {

    private final AssetService assetService;
    private final LiabilityService liabilityService;
    private final FinancialDataFreshnessCalculator calculator;

    public FinancialDataFreshnessService(
            AssetService assetService, LiabilityService liabilityService, FinancialDataFreshnessCalculator calculator
    ) {
        this.assetService = assetService;
        this.liabilityService = liabilityService;
        this.calculator = calculator;
    }

    public FinancialDataFreshnessResult review(UUID householdId, LocalDate reviewDate, int maxAgeDays) {
        List<Asset> assets = assetService.listAssets(householdId);
        List<Liability> liabilities = liabilityService.listLiabilities(householdId);

        List<FreshnessSourceRecord> sourceRecords = new ArrayList<>(assets.size() + liabilities.size());
        for (Asset asset : assets) {
            sourceRecords.add(new FreshnessSourceRecord(
                    asset.getId(),
                    FreshnessRecordKind.ASSET,
                    asset.getName(),
                    asset.getCurrency(),
                    asset.getSourceType(),
                    asset.getValuedAt()));
        }
        for (Liability liability : liabilities) {
            sourceRecords.add(new FreshnessSourceRecord(
                    liability.getId(),
                    FreshnessRecordKind.LIABILITY,
                    liability.getName(),
                    liability.getCurrency(),
                    liability.getSourceType(),
                    liability.getBalanceAsOf()));
        }

        return calculator.review(householdId, reviewDate, maxAgeDays, sourceRecords);
    }
}
