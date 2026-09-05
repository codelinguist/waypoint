package com.waypoint.household;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class FinancialSnapshotServiceTest {

    private final HouseholdRepository householdRepository = mock(HouseholdRepository.class);
    private final AssetRepository assetRepository = mock(AssetRepository.class);
    private final LiabilityRepository liabilityRepository = mock(LiabilityRepository.class);
    private final FinancialSnapshotRepository financialSnapshotRepository = mock(FinancialSnapshotRepository.class);
    private final SnapshotAssetLineItemRepository snapshotAssetLineItemRepository =
            mock(SnapshotAssetLineItemRepository.class);
    private final SnapshotLiabilityLineItemRepository snapshotLiabilityLineItemRepository =
            mock(SnapshotLiabilityLineItemRepository.class);
    private final FinancialSnapshotService financialSnapshotService = new FinancialSnapshotService(
            householdRepository, assetRepository, liabilityRepository, financialSnapshotRepository,
            snapshotAssetLineItemRepository, snapshotLiabilityLineItemRepository);

    @Test
    void throwsNotFoundWhenCreatingSnapshotForUnknownHousehold() {
        UUID householdId = UUID.randomUUID();
        when(householdRepository.findById(householdId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> financialSnapshotService.createSnapshot(householdId, LocalDate.now()))
                .isInstanceOf(HouseholdNotFoundException.class);
    }

    @Test
    void excludesAssetsAndLiabilitiesDatedAfterAsOfDate() {
        UUID householdId = UUID.randomUUID();
        Household household = new Household("Ralph Household", "PHP");
        LocalDate asOfDate = LocalDate.now();
        Asset eligibleAsset = new Asset(household, "Cash", AssetType.CASH, BigDecimal.TEN, BigDecimal.TEN, "PHP",
                asOfDate, Liquidity.LIQUID);
        Asset futureAsset = new Asset(household, "Future Fund", AssetType.CASH, BigDecimal.TEN, BigDecimal.TEN,
                "PHP", asOfDate.plusDays(1), Liquidity.LIQUID);
        Liability eligibleLiability = new Liability(household, "Loan", LiabilityType.PERSONAL_LOAN, BigDecimal.ONE,
                "PHP", asOfDate);
        Liability futureLiability = new Liability(household, "Future Loan", LiabilityType.PERSONAL_LOAN,
                BigDecimal.ONE, "PHP", asOfDate.plusDays(1));

        when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));
        when(financialSnapshotRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(assetRepository.findByHousehold_IdOrderByCreatedAtAscIdAsc(householdId))
                .thenReturn(List.of(eligibleAsset, futureAsset));
        when(liabilityRepository.findByHousehold_IdOrderByCreatedAtAscIdAsc(householdId))
                .thenReturn(List.of(eligibleLiability, futureLiability));
        when(snapshotAssetLineItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(snapshotLiabilityLineItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        FinancialSnapshotDetail detail = financialSnapshotService.createSnapshot(householdId, asOfDate);

        assertThat(detail.assetLineItems()).hasSize(1);
        assertThat(detail.assetLineItems().get(0).getName()).isEqualTo("Cash");
        assertThat(detail.liabilityLineItems()).hasSize(1);
        assertThat(detail.liabilityLineItems().get(0).getName()).isEqualTo("Loan");
    }

    @Test
    void computesPerCurrencyTotalsWithoutCombiningCurrencies() {
        UUID householdId = UUID.randomUUID();
        Household household = new Household("Ralph Household", "PHP");
        LocalDate asOfDate = LocalDate.now();
        Asset phpAsset = new Asset(household, "PHP Cash", AssetType.CASH, new BigDecimal("100.00"),
                new BigDecimal("100.00"), "PHP", asOfDate, Liquidity.LIQUID);
        Asset usdAsset = new Asset(household, "USD Cash", AssetType.CASH, new BigDecimal("50.00"),
                new BigDecimal("50.00"), "USD", asOfDate, Liquidity.LIQUID);
        Liability phpLiability = new Liability(household, "PHP Loan", LiabilityType.PERSONAL_LOAN,
                new BigDecimal("30.00"), "PHP", asOfDate);

        when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));
        when(financialSnapshotRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(assetRepository.findByHousehold_IdOrderByCreatedAtAscIdAsc(householdId))
                .thenReturn(List.of(phpAsset, usdAsset));
        when(liabilityRepository.findByHousehold_IdOrderByCreatedAtAscIdAsc(householdId))
                .thenReturn(List.of(phpLiability));
        when(snapshotAssetLineItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(snapshotLiabilityLineItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        FinancialSnapshotDetail detail = financialSnapshotService.createSnapshot(householdId, asOfDate);

        assertThat(detail.totalsByCurrency()).hasSize(2);
        CurrencyTotals php = detail.totalsByCurrency().stream()
                .filter(t -> t.currency().equals("PHP")).findFirst().orElseThrow();
        assertThat(php.assetTotal()).isEqualByComparingTo("100.00");
        assertThat(php.liabilityTotal()).isEqualByComparingTo("30.00");
        assertThat(php.netWorth()).isEqualByComparingTo("70.00");
        CurrencyTotals usd = detail.totalsByCurrency().stream()
                .filter(t -> t.currency().equals("USD")).findFirst().orElseThrow();
        assertThat(usd.assetTotal()).isEqualByComparingTo("50.00");
        assertThat(usd.liabilityTotal()).isEqualByComparingTo("0");
        assertThat(usd.netWorth()).isEqualByComparingTo("50.00");
    }

    @Test
    void returnsEmptyZeroTotalSnapshotWhenHouseholdHasNoEligibleRecords() {
        UUID householdId = UUID.randomUUID();
        Household household = new Household("Ralph Household", "PHP");
        when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));
        when(financialSnapshotRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(assetRepository.findByHousehold_IdOrderByCreatedAtAscIdAsc(householdId)).thenReturn(List.of());
        when(liabilityRepository.findByHousehold_IdOrderByCreatedAtAscIdAsc(householdId)).thenReturn(List.of());
        when(snapshotAssetLineItemRepository.saveAll(any())).thenReturn(List.of());
        when(snapshotLiabilityLineItemRepository.saveAll(any())).thenReturn(List.of());

        FinancialSnapshotDetail detail = financialSnapshotService.createSnapshot(householdId, LocalDate.now());

        assertThat(detail.assetLineItems()).isEmpty();
        assertThat(detail.liabilityLineItems()).isEmpty();
        assertThat(detail.totalsByCurrency()).isEmpty();
    }

    @Test
    void throwsNotFoundWhenGettingSnapshotForUnknownHousehold() {
        UUID householdId = UUID.randomUUID();
        when(householdRepository.existsById(householdId)).thenReturn(false);

        assertThatThrownBy(() -> financialSnapshotService.getSnapshot(householdId, UUID.randomUUID()))
                .isInstanceOf(HouseholdNotFoundException.class);
    }

    @Test
    void throwsNotFoundWhenSnapshotBelongsToAnotherHousehold() {
        UUID householdId = UUID.randomUUID();
        UUID snapshotId = UUID.randomUUID();
        when(householdRepository.existsById(householdId)).thenReturn(true);
        when(financialSnapshotRepository.findByIdAndHousehold_Id(snapshotId, householdId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> financialSnapshotService.getSnapshot(householdId, snapshotId))
                .isInstanceOf(FinancialSnapshotNotFoundException.class);
    }

    @Test
    void throwsNotFoundWhenListingSnapshotsForUnknownHousehold() {
        UUID householdId = UUID.randomUUID();
        when(householdRepository.existsById(householdId)).thenReturn(false);

        assertThatThrownBy(() -> financialSnapshotService.listSnapshots(householdId))
                .isInstanceOf(HouseholdNotFoundException.class);
    }

    @Test
    void listsSnapshotsInAscendingAsOfDateOrderForKnownHousehold() {
        UUID householdId = UUID.randomUUID();
        when(householdRepository.existsById(householdId)).thenReturn(true);
        when(financialSnapshotRepository.findByHousehold_IdOrderByAsOfDateAscCapturedAtAscIdAsc(householdId))
                .thenReturn(List.of());

        assertThat(financialSnapshotService.listSnapshots(householdId)).isEmpty();
    }

    @Test
    void throwsNotFoundWhenComparingSnapshotsForUnknownHousehold() {
        UUID householdId = UUID.randomUUID();
        when(householdRepository.existsById(householdId)).thenReturn(false);

        assertThatThrownBy(() -> financialSnapshotService.compareSnapshots(
                householdId, UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(HouseholdNotFoundException.class);
    }

    @Test
    void rejectsComparingASnapshotAgainstItself() {
        UUID householdId = UUID.randomUUID();
        UUID snapshotId = UUID.randomUUID();
        when(householdRepository.existsById(householdId)).thenReturn(true);

        assertThatThrownBy(() -> financialSnapshotService.compareSnapshots(householdId, snapshotId, snapshotId))
                .isInstanceOf(IdenticalSnapshotComparisonException.class);
    }

    @Test
    void throwsNotFoundWhenEarlierSnapshotDoesNotBelongToHousehold() {
        UUID householdId = UUID.randomUUID();
        UUID earlierSnapshotId = UUID.randomUUID();
        UUID laterSnapshotId = UUID.randomUUID();
        when(householdRepository.existsById(householdId)).thenReturn(true);
        when(financialSnapshotRepository.findByIdAndHousehold_Id(earlierSnapshotId, householdId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> financialSnapshotService.compareSnapshots(
                householdId, earlierSnapshotId, laterSnapshotId))
                .isInstanceOf(FinancialSnapshotNotFoundException.class);
    }

    @Test
    void throwsNotFoundWhenLaterSnapshotDoesNotBelongToHousehold() {
        UUID householdId = UUID.randomUUID();
        UUID earlierSnapshotId = UUID.randomUUID();
        UUID laterSnapshotId = UUID.randomUUID();
        Household household = new Household("Ralph Household", "PHP");
        FinancialSnapshot earlierSnapshot = new FinancialSnapshot(household, LocalDate.now().minusDays(1));
        when(householdRepository.existsById(householdId)).thenReturn(true);
        when(financialSnapshotRepository.findByIdAndHousehold_Id(earlierSnapshotId, householdId))
                .thenReturn(Optional.of(earlierSnapshot));
        when(financialSnapshotRepository.findByIdAndHousehold_Id(laterSnapshotId, householdId))
                .thenReturn(Optional.empty());
        when(snapshotAssetLineItemRepository.findBySnapshot_IdOrderByCreatedAtAscIdAsc(any())).thenReturn(List.of());
        when(snapshotLiabilityLineItemRepository.findBySnapshot_IdOrderByCreatedAtAscIdAsc(any()))
                .thenReturn(List.of());

        assertThatThrownBy(() -> financialSnapshotService.compareSnapshots(
                householdId, earlierSnapshotId, laterSnapshotId))
                .isInstanceOf(FinancialSnapshotNotFoundException.class);
    }

    @Test
    void computesSignedLaterMinusEarlierDeltasPerCurrency() {
        UUID householdId = UUID.randomUUID();
        UUID earlierSnapshotId = UUID.randomUUID();
        UUID laterSnapshotId = UUID.randomUUID();
        Household household = new Household("Ralph Household", "PHP");
        FinancialSnapshot earlierSnapshot = new FinancialSnapshot(household, LocalDate.now().minusMonths(1));
        FinancialSnapshot laterSnapshot = new FinancialSnapshot(household, LocalDate.now());
        ReflectionTestUtils.setField(earlierSnapshot, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(laterSnapshot, "id", UUID.randomUUID());

        SnapshotAssetLineItem earlierPhpAsset = new SnapshotAssetLineItem(earlierSnapshot, UUID.randomUUID(), "Cash",
                AssetType.CASH, "PHP", earlierSnapshot.getAsOfDate(), new BigDecimal("100.00"));
        SnapshotLiabilityLineItem earlierPhpLiability = new SnapshotLiabilityLineItem(earlierSnapshot,
                UUID.randomUUID(), "Loan", LiabilityType.PERSONAL_LOAN, "PHP", earlierSnapshot.getAsOfDate(),
                new BigDecimal("30.00"));

        SnapshotAssetLineItem laterPhpAsset = new SnapshotAssetLineItem(laterSnapshot, UUID.randomUUID(), "Cash",
                AssetType.CASH, "PHP", laterSnapshot.getAsOfDate(), new BigDecimal("80.00"));
        SnapshotAssetLineItem laterUsdAsset = new SnapshotAssetLineItem(laterSnapshot, UUID.randomUUID(),
                "USD Cash", AssetType.CASH, "USD", laterSnapshot.getAsOfDate(), new BigDecimal("20.00"));
        SnapshotLiabilityLineItem laterPhpLiability = new SnapshotLiabilityLineItem(laterSnapshot, UUID.randomUUID(),
                "Loan", LiabilityType.PERSONAL_LOAN, "PHP", laterSnapshot.getAsOfDate(), new BigDecimal("30.00"));

        when(householdRepository.existsById(householdId)).thenReturn(true);
        when(financialSnapshotRepository.findByIdAndHousehold_Id(earlierSnapshotId, householdId))
                .thenReturn(Optional.of(earlierSnapshot));
        when(financialSnapshotRepository.findByIdAndHousehold_Id(laterSnapshotId, householdId))
                .thenReturn(Optional.of(laterSnapshot));
        when(snapshotAssetLineItemRepository.findBySnapshot_IdOrderByCreatedAtAscIdAsc(earlierSnapshot.getId()))
                .thenReturn(List.of(earlierPhpAsset));
        when(snapshotLiabilityLineItemRepository.findBySnapshot_IdOrderByCreatedAtAscIdAsc(earlierSnapshot.getId()))
                .thenReturn(List.of(earlierPhpLiability));
        when(snapshotAssetLineItemRepository.findBySnapshot_IdOrderByCreatedAtAscIdAsc(laterSnapshot.getId()))
                .thenReturn(List.of(laterPhpAsset, laterUsdAsset));
        when(snapshotLiabilityLineItemRepository.findBySnapshot_IdOrderByCreatedAtAscIdAsc(laterSnapshot.getId()))
                .thenReturn(List.of(laterPhpLiability));

        FinancialSnapshotComparison comparison = financialSnapshotService.compareSnapshots(
                householdId, earlierSnapshotId, laterSnapshotId);

        assertThat(comparison.earlierSnapshot()).isSameAs(earlierSnapshot);
        assertThat(comparison.laterSnapshot()).isSameAs(laterSnapshot);
        assertThat(comparison.currencyDeltas()).hasSize(2);

        CurrencyTotalsDelta php = comparison.currencyDeltas().stream()
                .filter(delta -> delta.currency().equals("PHP")).findFirst().orElseThrow();
        assertThat(php.assetTotalDelta()).isEqualByComparingTo("-20.00");
        assertThat(php.liabilityTotalDelta()).isEqualByComparingTo("0.00");
        assertThat(php.netWorthDelta()).isEqualByComparingTo("-20.00");

        CurrencyTotalsDelta usd = comparison.currencyDeltas().stream()
                .filter(delta -> delta.currency().equals("USD")).findFirst().orElseThrow();
        assertThat(usd.assetTotalDelta()).isEqualByComparingTo("20.00");
        assertThat(usd.liabilityTotalDelta()).isEqualByComparingTo("0");
        assertThat(usd.netWorthDelta()).isEqualByComparingTo("20.00");
    }

    @Test
    void returnsZeroDeltasForIdenticalSnapshotContents() {
        UUID householdId = UUID.randomUUID();
        UUID earlierSnapshotId = UUID.randomUUID();
        UUID laterSnapshotId = UUID.randomUUID();
        Household household = new Household("Ralph Household", "PHP");
        FinancialSnapshot earlierSnapshot = new FinancialSnapshot(household, LocalDate.now().minusMonths(1));
        FinancialSnapshot laterSnapshot = new FinancialSnapshot(household, LocalDate.now());
        SnapshotAssetLineItem asset = new SnapshotAssetLineItem(earlierSnapshot, UUID.randomUUID(), "Cash",
                AssetType.CASH, "PHP", earlierSnapshot.getAsOfDate(), new BigDecimal("100.00"));

        when(householdRepository.existsById(householdId)).thenReturn(true);
        when(financialSnapshotRepository.findByIdAndHousehold_Id(earlierSnapshotId, householdId))
                .thenReturn(Optional.of(earlierSnapshot));
        when(financialSnapshotRepository.findByIdAndHousehold_Id(laterSnapshotId, householdId))
                .thenReturn(Optional.of(laterSnapshot));
        when(snapshotAssetLineItemRepository.findBySnapshot_IdOrderByCreatedAtAscIdAsc(any()))
                .thenReturn(List.of(asset));
        when(snapshotLiabilityLineItemRepository.findBySnapshot_IdOrderByCreatedAtAscIdAsc(any()))
                .thenReturn(List.of());

        FinancialSnapshotComparison comparison = financialSnapshotService.compareSnapshots(
                householdId, earlierSnapshotId, laterSnapshotId);

        assertThat(comparison.currencyDeltas()).hasSize(1);
        CurrencyTotalsDelta php = comparison.currencyDeltas().get(0);
        assertThat(php.assetTotalDelta()).isEqualByComparingTo("0.00");
        assertThat(php.liabilityTotalDelta()).isEqualByComparingTo("0");
        assertThat(php.netWorthDelta()).isEqualByComparingTo("0.00");
    }
}
