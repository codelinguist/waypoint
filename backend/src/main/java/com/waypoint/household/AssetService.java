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

    public AssetService(HouseholdRepository householdRepository, AssetRepository assetRepository) {
        this.householdRepository = householdRepository;
        this.assetRepository = assetRepository;
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
}
