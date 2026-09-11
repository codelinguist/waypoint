package com.waypoint.household;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AssetServiceTest {

    private final HouseholdRepository householdRepository = mock(HouseholdRepository.class);
    private final AssetRepository assetRepository = mock(AssetRepository.class);
    private final AssetValuationRepository assetValuationRepository = mock(AssetValuationRepository.class);
    private final AssetService assetService =
            new AssetService(householdRepository, assetRepository, assetValuationRepository);

    @Test
    void normalizesNameAndCurrencyOnCreate() {
        UUID householdId = UUID.randomUUID();
        Household household = new Household("Ralph Household", "PHP");
        when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));
        when(assetRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Asset created = assetService.createAsset(
                householdId,
                "  Emergency Fund  ",
                AssetType.CASH,
                new BigDecimal("1000.00"),
                new BigDecimal("1000.00"),
                "php",
                LocalDate.now(),
                Liquidity.LIQUID
        );

        assertThat(created.getName()).isEqualTo("Emergency Fund");
        assertThat(created.getCurrency()).isEqualTo("PHP");
        assertThat(created.getSourceType()).isEqualTo(SourceType.MANUAL_ENTRY);
        assertThat(created.getHousehold()).isSameAs(household);
    }

    @Test
    void throwsNotFoundWhenCreatingAssetForUnknownHousehold() {
        UUID householdId = UUID.randomUUID();
        when(householdRepository.findById(householdId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assetService.createAsset(
                householdId, "Fund", AssetType.CASH, BigDecimal.TEN, BigDecimal.TEN, "PHP",
                LocalDate.now(), Liquidity.LIQUID
        )).isInstanceOf(HouseholdNotFoundException.class);
    }

    @Test
    void rejectsPlanningValueGreaterThanEstimatedValue() {
        UUID householdId = UUID.randomUUID();
        Household household = new Household("Ralph Household", "PHP");
        when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));

        assertThatThrownBy(() -> assetService.createAsset(
                householdId, "Property", AssetType.PROPERTY,
                new BigDecimal("100.00"), new BigDecimal("150.00"), "PHP",
                LocalDate.now(), Liquidity.ILLIQUID
        )).isInstanceOf(InvalidAssetValueException.class);
    }

    @Test
    void returnsAssetScopedToHousehold() {
        UUID householdId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        Household household = new Household("Ralph Household", "PHP");
        Asset asset = new Asset(household, "Fund", AssetType.CASH, BigDecimal.TEN, BigDecimal.TEN, "PHP",
                LocalDate.now(), Liquidity.LIQUID);
        when(householdRepository.existsById(householdId)).thenReturn(true);
        when(assetRepository.findByIdAndHousehold_Id(assetId, householdId)).thenReturn(Optional.of(asset));

        assertThat(assetService.getAsset(householdId, assetId)).isSameAs(asset);
    }

    @Test
    void throwsNotFoundWhenAssetBelongsToAnotherHousehold() {
        UUID householdId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        when(householdRepository.existsById(householdId)).thenReturn(true);
        when(assetRepository.findByIdAndHousehold_Id(assetId, householdId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assetService.getAsset(householdId, assetId))
                .isInstanceOf(AssetNotFoundException.class);
    }

    @Test
    void throwsNotFoundWhenListingAssetsForUnknownHousehold() {
        UUID householdId = UUID.randomUUID();
        when(householdRepository.existsById(householdId)).thenReturn(false);

        assertThatThrownBy(() -> assetService.listAssets(householdId))
                .isInstanceOf(HouseholdNotFoundException.class);
    }

    @Test
    void listsAssetsInCreationOrderForKnownHousehold() {
        UUID householdId = UUID.randomUUID();
        when(householdRepository.existsById(householdId)).thenReturn(true);
        when(assetRepository.findByHousehold_IdOrderByCreatedAtAscIdAsc(householdId)).thenReturn(List.of());

        assertThat(assetService.listAssets(householdId)).isEmpty();
    }

    @Test
    void updateValuationAppliesChangeAndRecordsBeforeAfterHistory() {
        UUID householdId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        Household household = new Household("Ralph Household", "PHP");
        Asset before = new Asset(household, "Fund", AssetType.CASH, new BigDecimal("100.00"), new BigDecimal("90.00"),
                "PHP", LocalDate.of(2026, 1, 1), Liquidity.LIQUID);
        Asset after = new Asset(household, "Fund", AssetType.CASH, new BigDecimal("200.00"), new BigDecimal("150.00"),
                "PHP", LocalDate.of(2026, 2, 1), Liquidity.LIQUID);
        when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));
        when(assetRepository.findByIdAndHousehold_Id(assetId, householdId))
                .thenReturn(Optional.of(before), Optional.of(after));
        when(assetRepository.applyValuation(eq(assetId), eq(householdId), eq(new BigDecimal("200.00")),
                eq(new BigDecimal("150.00")), eq(LocalDate.of(2026, 2, 1)), any(Instant.class), eq(0L)))
                .thenReturn(1);

        Asset updated = assetService.updateValuation(
                householdId, assetId, new BigDecimal("200.00"), new BigDecimal("150.00"),
                LocalDate.of(2026, 2, 1), "Reappraised", 0L
        );

        assertThat(updated).isSameAs(after);

        ArgumentCaptor<AssetValuation> captor = ArgumentCaptor.forClass(AssetValuation.class);
        verify(assetValuationRepository).save(captor.capture());
        AssetValuation history = captor.getValue();
        assertThat(history.getPreviousEstimatedValue()).isEqualByComparingTo("100.00");
        assertThat(history.getPreviousPlanningValue()).isEqualByComparingTo("90.00");
        assertThat(history.getPreviousValuedAt()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(history.getNewEstimatedValue()).isEqualByComparingTo("200.00");
        assertThat(history.getNewPlanningValue()).isEqualByComparingTo("150.00");
        assertThat(history.getReason()).isEqualTo("Reappraised");
    }

    @Test
    void throwsStaleRevisionWhenNoRowMatchesTheExpectedRevision() {
        UUID householdId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        Household household = new Household("Ralph Household", "PHP");
        Asset asset = new Asset(household, "Fund", AssetType.CASH, BigDecimal.TEN, BigDecimal.TEN, "PHP",
                LocalDate.now(), Liquidity.LIQUID);
        when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));
        when(assetRepository.findByIdAndHousehold_Id(assetId, householdId)).thenReturn(Optional.of(asset));
        when(assetRepository.applyValuation(eq(assetId), eq(householdId), eq(BigDecimal.TEN), eq(BigDecimal.TEN),
                eq(LocalDate.now()), any(Instant.class), eq(5L))).thenReturn(0);

        assertThatThrownBy(() -> assetService.updateValuation(
                householdId, assetId, BigDecimal.TEN, BigDecimal.TEN, LocalDate.now(), "reason", 5L
        )).isInstanceOf(StaleAssetRevisionException.class);
    }

    @Test
    void rejectsPlanningValueGreaterThanEstimatedValueOnUpdate() {
        UUID householdId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        Household household = new Household("Ralph Household", "PHP");
        Asset asset = new Asset(household, "Fund", AssetType.CASH, BigDecimal.TEN, BigDecimal.TEN, "PHP",
                LocalDate.now(), Liquidity.LIQUID);
        when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));
        when(assetRepository.findByIdAndHousehold_Id(assetId, householdId)).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> assetService.updateValuation(
                householdId, assetId, new BigDecimal("100.00"), new BigDecimal("150.00"),
                LocalDate.now(), "reason", 0L
        )).isInstanceOf(InvalidAssetValueException.class);
    }

    @Test
    void throwsHouseholdNotFoundWhenUpdatingValuationForUnknownHousehold() {
        UUID householdId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        when(householdRepository.findById(householdId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assetService.updateValuation(
                householdId, assetId, BigDecimal.TEN, BigDecimal.TEN, LocalDate.now(), "reason", 0L
        )).isInstanceOf(HouseholdNotFoundException.class);
    }

    @Test
    void throwsAssetNotFoundWhenUpdatingValuationForAssetInAnotherHousehold() {
        UUID householdId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        Household household = new Household("Ralph Household", "PHP");
        when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));
        when(assetRepository.findByIdAndHousehold_Id(assetId, householdId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assetService.updateValuation(
                householdId, assetId, BigDecimal.TEN, BigDecimal.TEN, LocalDate.now(), "reason", 0L
        )).isInstanceOf(AssetNotFoundException.class);
    }

    @Test
    void listsValuationHistoryOrderedByRevisionForKnownAsset() {
        UUID householdId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        when(householdRepository.existsById(householdId)).thenReturn(true);
        Household household = new Household("Ralph Household", "PHP");
        Asset asset = new Asset(household, "Fund", AssetType.CASH, BigDecimal.TEN, BigDecimal.TEN, "PHP",
                LocalDate.now(), Liquidity.LIQUID);
        when(assetRepository.findByIdAndHousehold_Id(assetId, householdId)).thenReturn(Optional.of(asset));
        when(assetValuationRepository.findByAsset_IdAndAsset_Household_IdOrderByRevisionAsc(assetId, householdId))
                .thenReturn(List.of());

        assertThat(assetService.listValuationHistory(householdId, assetId)).isEmpty();
    }

    @Test
    void throwsHouseholdNotFoundWhenListingValuationHistoryForUnknownHousehold() {
        UUID householdId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        when(householdRepository.existsById(householdId)).thenReturn(false);

        assertThatThrownBy(() -> assetService.listValuationHistory(householdId, assetId))
                .isInstanceOf(HouseholdNotFoundException.class);
    }

    @Test
    void throwsAssetNotFoundWhenListingValuationHistoryForAssetInAnotherHousehold() {
        UUID householdId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        when(householdRepository.existsById(householdId)).thenReturn(true);
        when(assetRepository.findByIdAndHousehold_Id(assetId, householdId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assetService.listValuationHistory(householdId, assetId))
                .isInstanceOf(AssetNotFoundException.class);
    }
}
