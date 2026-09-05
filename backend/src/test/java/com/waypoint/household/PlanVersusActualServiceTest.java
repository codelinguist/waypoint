package com.waypoint.household;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PlanVersusActualServiceTest {

    private final FinancialSnapshotService financialSnapshotService = mock(FinancialSnapshotService.class);
    private final PlanVersusActualService planVersusActualService =
            new PlanVersusActualService(financialSnapshotService);

    @Test
    void propagatesHouseholdNotFoundFromSnapshotLookup() {
        UUID householdId = UUID.randomUUID();
        UUID snapshotId = UUID.randomUUID();
        when(financialSnapshotService.getSnapshot(householdId, snapshotId))
                .thenThrow(new HouseholdNotFoundException(householdId));

        assertThatThrownBy(() -> planVersusActualService.analyze(
                householdId, snapshotId, List.of(plannedTotals("PHP", "100.00", "30.00", "70.00"))))
                .isInstanceOf(HouseholdNotFoundException.class);
    }

    @Test
    void propagatesSnapshotNotFoundFromSnapshotLookup() {
        UUID householdId = UUID.randomUUID();
        UUID snapshotId = UUID.randomUUID();
        when(financialSnapshotService.getSnapshot(householdId, snapshotId))
                .thenThrow(new FinancialSnapshotNotFoundException(snapshotId));

        assertThatThrownBy(() -> planVersusActualService.analyze(
                householdId, snapshotId, List.of(plannedTotals("PHP", "100.00", "30.00", "70.00"))))
                .isInstanceOf(FinancialSnapshotNotFoundException.class);
    }

    @Test
    void rejectsDuplicatePlannedCurrenciesBeforeReadingTheSnapshot() {
        UUID householdId = UUID.randomUUID();
        UUID snapshotId = UUID.randomUUID();

        assertThatThrownBy(() -> planVersusActualService.analyze(
                householdId,
                snapshotId,
                List.of(
                        plannedTotals("PHP", "100.00", "30.00", "70.00"),
                        plannedTotals("php", "50.00", "10.00", "40.00"))))
                .isInstanceOf(InvalidPlanException.class);
    }

    @Test
    void rejectsNegativePlannedAssetTotal() {
        UUID householdId = UUID.randomUUID();
        UUID snapshotId = UUID.randomUUID();

        assertThatThrownBy(() -> planVersusActualService.analyze(
                householdId, snapshotId, List.of(plannedTotals("PHP", "-1.00", "0.00", "-1.00"))))
                .isInstanceOf(InvalidPlanException.class);
    }

    @Test
    void rejectsNegativePlannedLiabilityTotal() {
        UUID householdId = UUID.randomUUID();
        UUID snapshotId = UUID.randomUUID();

        assertThatThrownBy(() -> planVersusActualService.analyze(
                householdId, snapshotId, List.of(plannedTotals("PHP", "0.00", "-1.00", "1.00"))))
                .isInstanceOf(InvalidPlanException.class);
    }

    @Test
    void rejectsInconsistentPlannedNetWorth() {
        UUID householdId = UUID.randomUUID();
        UUID snapshotId = UUID.randomUUID();

        assertThatThrownBy(() -> planVersusActualService.analyze(
                householdId, snapshotId, List.of(plannedTotals("PHP", "100.00", "30.00", "50.00"))))
                .isInstanceOf(InvalidPlanException.class);
    }

    @Test
    void computesAboveBelowAndOnPlanDirectionsPerMeasure() {
        UUID householdId = UUID.randomUUID();
        UUID snapshotId = UUID.randomUUID();
        FinancialSnapshotDetail detail = snapshotDetailWithTotals(
                new CurrencyTotals("PHP", new BigDecimal("120.00"), new BigDecimal("30.00"), new BigDecimal("90.00")));
        when(financialSnapshotService.getSnapshot(householdId, snapshotId)).thenReturn(detail);

        PlanVersusActualAnalysis analysis = planVersusActualService.analyze(
                householdId, snapshotId, List.of(plannedTotals("PHP", "100.00", "30.00", "70.00")));

        CurrencyPlanVersusActual php = analysis.currencyResults().get(0);
        assertThat(php.assetTotal().planned()).isEqualByComparingTo("100.00");
        assertThat(php.assetTotal().actual()).isEqualByComparingTo("120.00");
        assertThat(php.assetTotal().variance()).isEqualByComparingTo("20.00");
        assertThat(php.assetTotal().direction()).isEqualTo(VarianceDirection.ABOVE_PLAN);

        assertThat(php.liabilityTotal().variance()).isEqualByComparingTo("0.00");
        assertThat(php.liabilityTotal().direction()).isEqualTo(VarianceDirection.ON_PLAN);

        assertThat(php.netWorth().variance()).isEqualByComparingTo("20.00");
        assertThat(php.netWorth().direction()).isEqualTo(VarianceDirection.ABOVE_PLAN);
    }

    @Test
    void reportsBelowPlanWhenActualIsLowerThanPlanned() {
        UUID householdId = UUID.randomUUID();
        UUID snapshotId = UUID.randomUUID();
        FinancialSnapshotDetail detail = snapshotDetailWithTotals(
                new CurrencyTotals("PHP", new BigDecimal("80.00"), new BigDecimal("30.00"), new BigDecimal("50.00")));
        when(financialSnapshotService.getSnapshot(householdId, snapshotId)).thenReturn(detail);

        PlanVersusActualAnalysis analysis = planVersusActualService.analyze(
                householdId, snapshotId, List.of(plannedTotals("PHP", "100.00", "30.00", "70.00")));

        CurrencyPlanVersusActual php = analysis.currencyResults().get(0);
        assertThat(php.assetTotal().variance()).isEqualByComparingTo("-20.00");
        assertThat(php.assetTotal().direction()).isEqualTo(VarianceDirection.BELOW_PLAN);
        assertThat(php.netWorth().variance()).isEqualByComparingTo("-20.00");
        assertThat(php.netWorth().direction()).isEqualTo(VarianceDirection.BELOW_PLAN);
    }

    @Test
    void treatsAPlannedCurrencyAbsentFromTheSnapshotAsZeroActual() {
        UUID householdId = UUID.randomUUID();
        UUID snapshotId = UUID.randomUUID();
        FinancialSnapshotDetail detail = snapshotDetailWithTotals();
        when(financialSnapshotService.getSnapshot(householdId, snapshotId)).thenReturn(detail);

        PlanVersusActualAnalysis analysis = planVersusActualService.analyze(
                householdId, snapshotId, List.of(plannedTotals("USD", "50.00", "0.00", "50.00")));

        CurrencyPlanVersusActual usd = analysis.currencyResults().get(0);
        assertThat(usd.assetTotal().actual()).isEqualByComparingTo("0");
        assertThat(usd.assetTotal().variance()).isEqualByComparingTo("-50.00");
        assertThat(usd.assetTotal().direction()).isEqualTo(VarianceDirection.BELOW_PLAN);
    }

    @Test
    void normalizesPlannedCurrencyCaseWhenMatchingActualTotals() {
        UUID householdId = UUID.randomUUID();
        UUID snapshotId = UUID.randomUUID();
        FinancialSnapshotDetail detail = snapshotDetailWithTotals(
                new CurrencyTotals("PHP", new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("100.00")));
        when(financialSnapshotService.getSnapshot(householdId, snapshotId)).thenReturn(detail);

        PlanVersusActualAnalysis analysis = planVersusActualService.analyze(
                householdId, snapshotId, List.of(plannedTotals("php", "100.00", "0.00", "100.00")));

        assertThat(analysis.currencyResults()).hasSize(1);
        assertThat(analysis.currencyResults().get(0).currency()).isEqualTo("PHP");
        assertThat(analysis.currencyResults().get(0).assetTotal().direction()).isEqualTo(VarianceDirection.ON_PLAN);
    }

    @Test
    void doesNotPersistAnythingOrMutateTheSnapshot() {
        UUID householdId = UUID.randomUUID();
        UUID snapshotId = UUID.randomUUID();
        FinancialSnapshotDetail detail = snapshotDetailWithTotals(
                new CurrencyTotals("PHP", new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("100.00")));
        when(financialSnapshotService.getSnapshot(householdId, snapshotId)).thenReturn(detail);

        PlanVersusActualAnalysis first = planVersusActualService.analyze(
                householdId, snapshotId, List.of(plannedTotals("PHP", "100.00", "0.00", "100.00")));
        PlanVersusActualAnalysis second = planVersusActualService.analyze(
                householdId, snapshotId, List.of(plannedTotals("PHP", "100.00", "0.00", "100.00")));

        assertThat(first).isEqualTo(second);
        org.mockito.Mockito.verify(financialSnapshotService, org.mockito.Mockito.times(2)).getSnapshot(householdId, snapshotId);
        org.mockito.Mockito.verifyNoMoreInteractions(financialSnapshotService);
    }

    private FinancialSnapshotDetail snapshotDetailWithTotals(CurrencyTotals... totals) {
        Household household = new Household("Ralph Household", "PHP");
        FinancialSnapshot snapshot = new FinancialSnapshot(household, LocalDate.now());
        ReflectionTestUtils.setField(snapshot, "id", UUID.randomUUID());
        return new FinancialSnapshotDetail(snapshot, List.of(), List.of(), List.of(totals));
    }

    private PlannedCurrencyTotals plannedTotals(
            String currency, String assetTotal, String liabilityTotal, String netWorth
    ) {
        return new PlannedCurrencyTotals(
                currency, new BigDecimal(assetTotal), new BigDecimal(liabilityTotal), new BigDecimal(netWorth));
    }
}
