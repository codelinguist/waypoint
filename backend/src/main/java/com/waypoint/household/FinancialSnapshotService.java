package com.waypoint.household;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FinancialSnapshotService {

    private final HouseholdRepository householdRepository;
    private final AssetRepository assetRepository;
    private final LiabilityRepository liabilityRepository;
    private final FinancialSnapshotRepository financialSnapshotRepository;
    private final SnapshotAssetLineItemRepository snapshotAssetLineItemRepository;
    private final SnapshotLiabilityLineItemRepository snapshotLiabilityLineItemRepository;

    public FinancialSnapshotService(
            HouseholdRepository householdRepository,
            AssetRepository assetRepository,
            LiabilityRepository liabilityRepository,
            FinancialSnapshotRepository financialSnapshotRepository,
            SnapshotAssetLineItemRepository snapshotAssetLineItemRepository,
            SnapshotLiabilityLineItemRepository snapshotLiabilityLineItemRepository
    ) {
        this.householdRepository = householdRepository;
        this.assetRepository = assetRepository;
        this.liabilityRepository = liabilityRepository;
        this.financialSnapshotRepository = financialSnapshotRepository;
        this.snapshotAssetLineItemRepository = snapshotAssetLineItemRepository;
        this.snapshotLiabilityLineItemRepository = snapshotLiabilityLineItemRepository;
    }

    public FinancialSnapshotDetail createSnapshot(UUID householdId, LocalDate asOfDate) {
        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new HouseholdNotFoundException(householdId));

        FinancialSnapshot snapshot = financialSnapshotRepository.save(new FinancialSnapshot(household, asOfDate));

        List<Asset> eligibleAssets = assetRepository.findByHousehold_IdOrderByCreatedAtAscIdAsc(householdId).stream()
                .filter(asset -> !asset.getValuedAt().isAfter(asOfDate))
                .toList();
        List<SnapshotAssetLineItem> assetLineItems = snapshotAssetLineItemRepository.saveAll(
                eligibleAssets.stream()
                        .map(asset -> new SnapshotAssetLineItem(
                                snapshot,
                                asset.getId(),
                                asset.getName(),
                                asset.getAssetType(),
                                asset.getCurrency(),
                                asset.getValuedAt(),
                                asset.getPlanningValue()
                        ))
                        .toList()
        );

        List<Liability> eligibleLiabilities = liabilityRepository
                .findByHousehold_IdOrderByCreatedAtAscIdAsc(householdId).stream()
                .filter(liability -> !liability.getBalanceAsOf().isAfter(asOfDate))
                .toList();
        List<SnapshotLiabilityLineItem> liabilityLineItems = snapshotLiabilityLineItemRepository.saveAll(
                eligibleLiabilities.stream()
                        .map(liability -> new SnapshotLiabilityLineItem(
                                snapshot,
                                liability.getId(),
                                liability.getName(),
                                liability.getLiabilityType(),
                                liability.getCurrency(),
                                liability.getBalanceAsOf(),
                                liability.getOutstandingBalance()
                        ))
                        .toList()
        );

        return new FinancialSnapshotDetail(
                snapshot, assetLineItems, liabilityLineItems, computeTotals(assetLineItems, liabilityLineItems));
    }

    @Transactional(readOnly = true)
    public FinancialSnapshotDetail getSnapshot(UUID householdId, UUID snapshotId) {
        if (!householdRepository.existsById(householdId)) {
            throw new HouseholdNotFoundException(householdId);
        }
        FinancialSnapshot snapshot = financialSnapshotRepository.findByIdAndHousehold_Id(snapshotId, householdId)
                .orElseThrow(() -> new FinancialSnapshotNotFoundException(snapshotId));
        return toDetail(snapshot);
    }

    @Transactional(readOnly = true)
    public List<FinancialSnapshotDetail> listSnapshots(UUID householdId) {
        if (!householdRepository.existsById(householdId)) {
            throw new HouseholdNotFoundException(householdId);
        }
        return financialSnapshotRepository.findByHousehold_IdOrderByAsOfDateAscCapturedAtAscIdAsc(householdId)
                .stream()
                .map(this::toDetail)
                .toList();
    }

    @Transactional(readOnly = true)
    public FinancialSnapshotComparison compareSnapshots(
            UUID householdId, UUID earlierSnapshotId, UUID laterSnapshotId
    ) {
        if (!householdRepository.existsById(householdId)) {
            throw new HouseholdNotFoundException(householdId);
        }
        if (earlierSnapshotId.equals(laterSnapshotId)) {
            throw new IdenticalSnapshotComparisonException(earlierSnapshotId);
        }
        FinancialSnapshot earlierSnapshot = financialSnapshotRepository
                .findByIdAndHousehold_Id(earlierSnapshotId, householdId)
                .orElseThrow(() -> new FinancialSnapshotNotFoundException(earlierSnapshotId));
        FinancialSnapshot laterSnapshot = financialSnapshotRepository
                .findByIdAndHousehold_Id(laterSnapshotId, householdId)
                .orElseThrow(() -> new FinancialSnapshotNotFoundException(laterSnapshotId));

        List<CurrencyTotals> earlierTotals = toDetail(earlierSnapshot).totalsByCurrency();
        List<CurrencyTotals> laterTotals = toDetail(laterSnapshot).totalsByCurrency();
        return new FinancialSnapshotComparison(
                earlierSnapshot, laterSnapshot, computeDeltas(earlierTotals, laterTotals));
    }

    private List<CurrencyTotalsDelta> computeDeltas(
            List<CurrencyTotals> earlierTotals, List<CurrencyTotals> laterTotals
    ) {
        Map<String, CurrencyTotals> earlierByCurrency = new TreeMap<>();
        earlierTotals.forEach(totals -> earlierByCurrency.put(totals.currency(), totals));
        Map<String, CurrencyTotals> laterByCurrency = new TreeMap<>();
        laterTotals.forEach(totals -> laterByCurrency.put(totals.currency(), totals));

        TreeSet<String> currencies = new TreeSet<>();
        currencies.addAll(earlierByCurrency.keySet());
        currencies.addAll(laterByCurrency.keySet());

        List<CurrencyTotalsDelta> deltas = new ArrayList<>();
        for (String currency : currencies) {
            CurrencyTotals earlier = earlierByCurrency.getOrDefault(currency, zeroTotals(currency));
            CurrencyTotals later = laterByCurrency.getOrDefault(currency, zeroTotals(currency));
            deltas.add(CurrencyTotalsDelta.of(currency, earlier, later));
        }
        return deltas;
    }

    private CurrencyTotals zeroTotals(String currency) {
        return new CurrencyTotals(currency, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    private FinancialSnapshotDetail toDetail(FinancialSnapshot snapshot) {
        List<SnapshotAssetLineItem> assetLineItems =
                snapshotAssetLineItemRepository.findBySnapshot_IdOrderByCreatedAtAscIdAsc(snapshot.getId());
        List<SnapshotLiabilityLineItem> liabilityLineItems =
                snapshotLiabilityLineItemRepository.findBySnapshot_IdOrderByCreatedAtAscIdAsc(snapshot.getId());
        return new FinancialSnapshotDetail(
                snapshot, assetLineItems, liabilityLineItems, computeTotals(assetLineItems, liabilityLineItems));
    }

    private List<CurrencyTotals> computeTotals(
            List<SnapshotAssetLineItem> assetLineItems, List<SnapshotLiabilityLineItem> liabilityLineItems
    ) {
        Map<String, BigDecimal> assetTotals = new TreeMap<>();
        for (SnapshotAssetLineItem item : assetLineItems) {
            assetTotals.merge(item.getCurrency(), item.getValue(), BigDecimal::add);
        }
        Map<String, BigDecimal> liabilityTotals = new TreeMap<>();
        for (SnapshotLiabilityLineItem item : liabilityLineItems) {
            liabilityTotals.merge(item.getCurrency(), item.getValue(), BigDecimal::add);
        }

        TreeSet<String> currencies = new TreeSet<>();
        currencies.addAll(assetTotals.keySet());
        currencies.addAll(liabilityTotals.keySet());

        List<CurrencyTotals> totals = new ArrayList<>();
        for (String currency : currencies) {
            BigDecimal assetTotal = assetTotals.getOrDefault(currency, BigDecimal.ZERO);
            BigDecimal liabilityTotal = liabilityTotals.getOrDefault(currency, BigDecimal.ZERO);
            totals.add(new CurrencyTotals(currency, assetTotal, liabilityTotal, assetTotal.subtract(liabilityTotal)));
        }
        return totals;
    }
}
