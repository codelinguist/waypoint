package com.waypoint.position;

import static org.assertj.core.api.Assertions.assertThat;

import com.waypoint.household.Asset;
import com.waypoint.household.AssetType;
import com.waypoint.household.CurrencyTotals;
import com.waypoint.household.Household;
import com.waypoint.household.Liability;
import com.waypoint.household.LiabilityType;
import com.waypoint.household.Liquidity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Pure domain tests for {@link CurrencyTotalsCalculator}, called directly with
 * no Spring context or database.
 */
class CurrencyTotalsCalculatorTest {

    private static final Household HOUSEHOLD = new Household("Test Household", "PHP");

    @Test
    void reconcilesMixedCurrencyAssetsAndLiabilities() {
        List<Asset> assets = List.of(
                asset("100.00", "PHP"),
                asset("50.00", "PHP"),
                asset("200.00", "USD"));
        List<Liability> liabilities = List.of(
                liability("30.00", "PHP"),
                liability("20.00", "USD"));

        List<CurrencyTotals> totals = CurrencyTotalsCalculator.compute(assets, liabilities);

        assertThat(totals).containsExactly(
                new CurrencyTotals("PHP", new BigDecimal("150.00"), new BigDecimal("30.00"), new BigDecimal("120.00")),
                new CurrencyTotals("USD", new BigDecimal("200.00"), new BigDecimal("20.00"), new BigDecimal("180.00")));
    }

    @Test
    void assetOnlyCurrencyHasExactZeroLiabilityTotal() {
        List<CurrencyTotals> totals = CurrencyTotalsCalculator.compute(
                List.of(asset("100.00", "PHP")), List.of());

        assertThat(totals).containsExactly(
                new CurrencyTotals("PHP", new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("100.00")));
    }

    @Test
    void liabilityOnlyCurrencyHasExactZeroAssetTotal() {
        List<CurrencyTotals> totals = CurrencyTotalsCalculator.compute(
                List.of(), List.of(liability("40.00", "PHP")));

        assertThat(totals).containsExactly(
                new CurrencyTotals("PHP", BigDecimal.ZERO, new BigDecimal("40.00"), new BigDecimal("-40.00")));
    }

    @Test
    void emptyRowsProduceEmptyTotals() {
        assertThat(CurrencyTotalsCalculator.compute(List.of(), List.of())).isEmpty();
    }

    @Test
    void negativeNetWorthWhenLiabilitiesExceedAssets() {
        List<CurrencyTotals> totals = CurrencyTotalsCalculator.compute(
                List.of(asset("100.00", "PHP")), List.of(liability("250.00", "PHP")));

        assertThat(totals).containsExactly(
                new CurrencyTotals("PHP", new BigDecimal("100.00"), new BigDecimal("250.00"), new BigDecimal("-150.00")));
    }

    @Test
    void zeroValuedRowsAreIncludedInTotals() {
        List<CurrencyTotals> totals = CurrencyTotalsCalculator.compute(
                List.of(asset("0.00", "PHP")), List.of(liability("0.00", "PHP")));

        assertThat(totals).containsExactly(
                new CurrencyTotals("PHP", new BigDecimal("0.00"), new BigDecimal("0.00"), new BigDecimal("0.00")));
    }

    @Test
    void doesNotTruncateOrOverflowWhenSummingValuesAboveIndividualStoragePrecision() {
        BigDecimal maxStorable = new BigDecimal("99999999999999999.99");
        List<CurrencyTotals> totals = CurrencyTotalsCalculator.compute(
                List.of(asset(maxStorable.toPlainString(), "PHP"), asset(maxStorable.toPlainString(), "PHP")),
                List.of());

        assertThat(totals).hasSize(1);
        assertThat(totals.get(0).assetTotal()).isEqualByComparingTo(new BigDecimal("199999999999999999.98"));
        assertThat(totals.get(0).netWorth()).isEqualByComparingTo(new BigDecimal("199999999999999999.98"));
    }

    @Test
    void noAllCurrencySumOrImpliedConversionAcrossCurrencies() {
        List<CurrencyTotals> totals = CurrencyTotalsCalculator.compute(
                List.of(asset("100.00", "PHP"), asset("100.00", "USD")), List.of());

        assertThat(totals).hasSize(2);
        assertThat(totals).extracting(CurrencyTotals::currency).containsExactly("PHP", "USD");
    }

    @Test
    void totalsAreOrderedByCurrency() {
        List<CurrencyTotals> totals = CurrencyTotalsCalculator.compute(
                List.of(asset("1.00", "USD"), asset("1.00", "EUR"), asset("1.00", "PHP")), List.of());

        assertThat(totals).extracting(CurrencyTotals::currency).containsExactly("EUR", "PHP", "USD");
    }

    private static Asset asset(String planningValue, String currency) {
        BigDecimal value = new BigDecimal(planningValue);
        return new Asset(
                HOUSEHOLD, "Asset", AssetType.CASH, value, value, currency, LocalDate.now(), Liquidity.LIQUID);
    }

    private static Liability liability(String outstandingBalance, String currency) {
        return new Liability(
                HOUSEHOLD, "Liability", LiabilityType.OTHER, new BigDecimal(outstandingBalance), currency,
                LocalDate.now());
    }
}
