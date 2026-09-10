package com.waypoint.household;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AssetService {

    private final HouseholdRepository householdRepository;
    private final AssetRepository assetRepository;
    private final AssetValuationRepository assetValuationRepository;

    public AssetService(
            HouseholdRepository householdRepository,
            AssetRepository assetRepository,
            AssetValuationRepository assetValuationRepository
    ) {
        this.householdRepository = householdRepository;
        this.assetRepository = assetRepository;
        this.assetValuationRepository = assetValuationRepository;
    }

    public Asset createAsset(
            UUID householdId,
            String name,
            AssetType assetType,
            BigDecimal estimatedValue,
            BigDecimal planningValue,
            String currency,
            LocalDate valuedAt,
            Liquidity liquidity
    ) {
        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new HouseholdNotFoundException(householdId));
        if (planningValue.compareTo(estimatedValue) > 0) {
            throw new InvalidAssetValueException("planningValue must not exceed estimatedValue");
        }
        Asset asset = new Asset(
                household,
                name.trim(),
                assetType,
                estimatedValue,
                planningValue,
                currency.trim().toUpperCase(),
                valuedAt,
                liquidity
        );
        return assetRepository.save(asset);
    }

    @Transactional(readOnly = true)
    public Asset getAsset(UUID householdId, UUID assetId) {
        if (!householdRepository.existsById(householdId)) {
            throw new HouseholdNotFoundException(householdId);
        }
        return assetRepository.findByIdAndHousehold_Id(assetId, householdId)
                .orElseThrow(() -> new AssetNotFoundException(assetId));
    }

    @Transactional(readOnly = true)
    public List<Asset> listAssets(UUID householdId) {
        if (!householdRepository.existsById(householdId)) {
            throw new HouseholdNotFoundException(householdId);
        }
        return assetRepository.findByHousehold_IdOrderByCreatedAtAscIdAsc(householdId);
    }

    /**
     * Atomically replaces an asset's recorded valuation and appends the
     * before/after audit evidence. {@code expectedRevision} guards against
     * lost updates: {@link AssetRepository#applyValuation} conditions its
     * single update statement on the row's current revision still matching,
     * so a caller-stale revision and a genuine concurrent conflicting write
     * both surface identically as zero rows updated.
     */
    public Asset updateValuation(
            UUID householdId,
            UUID assetId,
            BigDecimal estimatedValue,
            BigDecimal planningValue,
            LocalDate valuedAt,
            String reason,
            long expectedRevision
    ) {
        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new HouseholdNotFoundException(householdId));
        Asset asset = assetRepository.findByIdAndHousehold_Id(assetId, householdId)
                .orElseThrow(() -> new AssetNotFoundException(assetId));
        if (planningValue.compareTo(estimatedValue) > 0) {
            throw new InvalidAssetValueException("planningValue must not exceed estimatedValue");
        }

        BigDecimal previousEstimatedValue = asset.getEstimatedValue();
        BigDecimal previousPlanningValue = asset.getPlanningValue();
        LocalDate previousValuedAt = asset.getValuedAt();
        SourceType previousSourceType = asset.getSourceType();

        int updatedRows = assetRepository.applyValuation(
                assetId, householdId, estimatedValue, planningValue, valuedAt, expectedRevision);
        if (updatedRows == 0) {
            throw new StaleAssetRevisionException(assetId, expectedRevision);
        }

        Asset saved = assetRepository.findByIdAndHousehold_Id(assetId, householdId)
                .orElseThrow(() -> new AssetNotFoundException(assetId));

        assetValuationRepository.save(new AssetValuation(
                saved,
                household,
                saved.getRevision(),
                previousEstimatedValue,
                previousPlanningValue,
                previousValuedAt,
                previousSourceType,
                saved.getEstimatedValue(),
                saved.getPlanningValue(),
                saved.getValuedAt(),
                saved.getSourceType(),
                reason.trim()
        ));

        return saved;
    }

    @Transactional(readOnly = true)
    public List<AssetValuation> listValuationHistory(UUID householdId, UUID assetId) {
        if (!householdRepository.existsById(householdId)) {
            throw new HouseholdNotFoundException(householdId);
        }
        assetRepository.findByIdAndHousehold_Id(assetId, householdId)
                .orElseThrow(() -> new AssetNotFoundException(assetId));
        return assetValuationRepository.findByAsset_IdAndAsset_Household_IdOrderByRevisionAsc(assetId, householdId);
    }
}
