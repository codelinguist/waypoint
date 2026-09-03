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

class AssetServiceTest {

    private final HouseholdRepository householdRepository = mock(HouseholdRepository.class);
    private final AssetRepository assetRepository = mock(AssetRepository.class);
    private final AssetService assetService = new AssetService(householdRepository, assetRepository);

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
}
