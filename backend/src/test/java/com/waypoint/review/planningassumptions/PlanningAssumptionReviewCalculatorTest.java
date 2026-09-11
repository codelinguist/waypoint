package com.waypoint.review.planningassumptions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.waypoint.household.SourceType;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlanningAssumptionReviewCalculatorTest {

    private final PlanningAssumptionReviewCalculator calculator = new PlanningAssumptionReviewCalculator();

    private static PlanningAssumptionReviewSourceRecord assumption(
            UUID id, String name, LocalDate reviewDate, LocalDate effectiveFrom, LocalDate effectiveUntil
    ) {
        return new PlanningAssumptionReviewSourceRecord(
                id, name, reviewDate, effectiveFrom, effectiveUntil, SourceType.MANUAL_ENTRY);
    }

    @Test
    void classifiesReviewDatesBeforeEqualAndAfterAsOfAsOverdueDueTodayAndUpcoming() {
        LocalDate asOf = LocalDate.of(2026, 9, 10);
        LocalDate effectiveFrom = LocalDate.of(2026, 1, 1);

        PlanningAssumptionReviewResult result = calculator.review(UUID.randomUUID(), asOf, List.of(
                assumption(UUID.randomUUID(), "Overdue", LocalDate.of(2026, 9, 9), effectiveFrom, null),
                assumption(UUID.randomUUID(), "Due today", LocalDate.of(2026, 9, 10), effectiveFrom, null),
                assumption(UUID.randomUUID(), "Upcoming", LocalDate.of(2026, 9, 11), effectiveFrom, null)));

        assertThat(result.rows()).extracting(PlanningAssumptionReviewRow::reviewStatus)
                .containsExactly(ReviewStatus.OVERDUE, ReviewStatus.DUE_TODAY, ReviewStatus.UPCOMING);
    }

    @Test
    void effectiveUntilEqualToAsOfRemainsEffectiveAndTheNextDayIsExpired() {
        LocalDate effectiveFrom = LocalDate.of(2026, 1, 1);
        LocalDate effectiveUntil = LocalDate.of(2026, 9, 10);

        PlanningAssumptionReviewResult onEndDate = calculator.review(UUID.randomUUID(), effectiveUntil, List.of(
                assumption(UUID.randomUUID(), "Ends today", LocalDate.of(2026, 12, 1), effectiveFrom, effectiveUntil)));
        PlanningAssumptionReviewResult dayAfterEndDate = calculator.review(
                UUID.randomUUID(), effectiveUntil.plusDays(1), List.of(
                        assumption(UUID.randomUUID(), "Ends today", LocalDate.of(2026, 12, 1), effectiveFrom, effectiveUntil)));

        assertThat(onEndDate.rows().get(0).effectiveStatus()).isEqualTo(EffectiveStatus.EFFECTIVE);
        assertThat(dayAfterEndDate.rows().get(0).effectiveStatus()).isEqualTo(EffectiveStatus.EXPIRED);
    }

    @Test
    void nullEffectiveUntilNeverExpires() {
        LocalDate asOf = LocalDate.of(2099, 1, 1);

        PlanningAssumptionReviewResult result = calculator.review(UUID.randomUUID(), asOf, List.of(
                assumption(UUID.randomUUID(), "Open ended", LocalDate.of(2026, 1, 1), LocalDate.of(2020, 1, 1), null)));

        assertThat(result.rows().get(0).effectiveStatus()).isEqualTo(EffectiveStatus.EFFECTIVE);
    }

    @Test
    void futureEffectiveFromYieldsNotYetEffective() {
        LocalDate asOf = LocalDate.of(2026, 9, 10);

        PlanningAssumptionReviewResult result = calculator.review(UUID.randomUUID(), asOf, List.of(
                assumption(UUID.randomUUID(), "Future start", LocalDate.of(2026, 12, 1),
                        LocalDate.of(2026, 9, 11), null)));

        assertThat(result.rows().get(0).effectiveStatus()).isEqualTo(EffectiveStatus.NOT_YET_EFFECTIVE);
    }

    @Test
    void expiredAssumptionWithUpcomingReviewDateStillNeedsAttention() {
        LocalDate asOf = LocalDate.of(2026, 9, 10);

        PlanningAssumptionReviewResult result = calculator.review(UUID.randomUUID(), asOf, List.of(
                assumption(UUID.randomUUID(), "Expired but not due", LocalDate.of(2026, 12, 1),
                        LocalDate.of(2020, 1, 1), LocalDate.of(2026, 9, 9))));

        PlanningAssumptionReviewRow row = result.rows().get(0);
        assertThat(row.reviewStatus()).isEqualTo(ReviewStatus.UPCOMING);
        assertThat(row.effectiveStatus()).isEqualTo(EffectiveStatus.EXPIRED);
        assertThat(row.needsAttention()).isTrue();
    }

    @Test
    void futureEffectiveAssumptionWithDueReviewDateRetainsDueStatus() {
        LocalDate asOf = LocalDate.of(2026, 9, 10);

        PlanningAssumptionReviewResult result = calculator.review(UUID.randomUUID(), asOf, List.of(
                assumption(UUID.randomUUID(), "Not started but overdue review", LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 12, 1), null)));

        PlanningAssumptionReviewRow row = result.rows().get(0);
        assertThat(row.reviewStatus()).isEqualTo(ReviewStatus.OVERDUE);
        assertThat(row.effectiveStatus()).isEqualTo(EffectiveStatus.NOT_YET_EFFECTIVE);
        assertThat(row.needsAttention()).isTrue();
    }

    @Test
    void upcomingReviewAndEffectiveAssumptionDoesNotNeedAttention() {
        LocalDate asOf = LocalDate.of(2026, 9, 10);

        PlanningAssumptionReviewResult result = calculator.review(UUID.randomUUID(), asOf, List.of(
                assumption(UUID.randomUUID(), "All clear", LocalDate.of(2026, 12, 1),
                        LocalDate.of(2026, 1, 1), null)));

        assertThat(result.rows().get(0).needsAttention()).isFalse();
        assertThat(result.needsAttentionCount()).isZero();
    }

    @Test
    void classifiesLeapYearFebruary29BoundaryCorrectly() {
        LocalDate asOf = LocalDate.of(2024, 3, 1);
        LocalDate effectiveUntil = LocalDate.of(2024, 2, 29);

        PlanningAssumptionReviewResult result = calculator.review(UUID.randomUUID(), asOf, List.of(
                assumption(UUID.randomUUID(), "Leap boundary", LocalDate.of(2024, 3, 1),
                        LocalDate.of(2024, 1, 1), effectiveUntil)));

        assertThat(result.rows().get(0).effectiveStatus()).isEqualTo(EffectiveStatus.EXPIRED);
    }

    @Test
    void classifiesMonthBoundaryCorrectly() {
        LocalDate asOf = LocalDate.of(2026, 3, 1);

        PlanningAssumptionReviewResult result = calculator.review(UUID.randomUUID(), asOf, List.of(
                assumption(UUID.randomUUID(), "Month boundary", LocalDate.of(2026, 2, 28),
                        LocalDate.of(2026, 1, 1), null)));

        assertThat(result.rows().get(0).reviewStatus()).isEqualTo(ReviewStatus.OVERDUE);
    }

    @Test
    void classifiesYearBoundaryCorrectly() {
        LocalDate asOf = LocalDate.of(2026, 1, 1);

        PlanningAssumptionReviewResult result = calculator.review(UUID.randomUUID(), asOf, List.of(
                assumption(UUID.randomUUID(), "Year boundary", LocalDate.of(2025, 12, 31),
                        LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31))));

        PlanningAssumptionReviewRow row = result.rows().get(0);
        assertThat(row.reviewStatus()).isEqualTo(ReviewStatus.OVERDUE);
        assertThat(row.effectiveStatus()).isEqualTo(EffectiveStatus.EXPIRED);
    }

    @Test
    void ordersRowsByReviewDateThenById() {
        LocalDate asOf = LocalDate.of(2026, 9, 10);
        UUID lowId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID highId = UUID.fromString("00000000-0000-0000-0000-000000000002");

        PlanningAssumptionReviewResult result = calculator.review(UUID.randomUUID(), asOf, List.of(
                assumption(highId, "Later, high id", LocalDate.of(2026, 9, 12), LocalDate.of(2026, 1, 1), null),
                assumption(highId, "Same date, high id", LocalDate.of(2026, 9, 11), LocalDate.of(2026, 1, 1), null),
                assumption(lowId, "Same date, low id", LocalDate.of(2026, 9, 11), LocalDate.of(2026, 1, 1), null)));

        assertThat(result.rows()).extracting(PlanningAssumptionReviewRow::assumptionId)
                .containsExactly(lowId, highId, highId);
    }

    @Test
    void countsAndTotalsAreDerivedFromExactlyTheReturnedRowsIncludingZeroes() {
        LocalDate asOf = LocalDate.of(2026, 9, 10);

        PlanningAssumptionReviewResult result = calculator.review(UUID.randomUUID(), asOf, List.of(
                assumption(UUID.randomUUID(), "Overdue", LocalDate.of(2026, 9, 9), LocalDate.of(2026, 1, 1), null),
                assumption(UUID.randomUUID(), "Overdue too", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 1, 1), null)));

        assertThat(result.totalCount()).isEqualTo(2);
        assertThat(result.needsAttentionCount()).isEqualTo(2);
        assertThat(result.countsByReviewStatus())
                .containsEntry(ReviewStatus.OVERDUE, 2)
                .containsEntry(ReviewStatus.DUE_TODAY, 0)
                .containsEntry(ReviewStatus.UPCOMING, 0);
        assertThat(result.countsByEffectiveStatus())
                .containsEntry(EffectiveStatus.EFFECTIVE, 2)
                .containsEntry(EffectiveStatus.NOT_YET_EFFECTIVE, 0)
                .containsEntry(EffectiveStatus.EXPIRED, 0);
    }

    @Test
    void emptySourceRecordsYieldEmptyRowsAndZeroCounts() {
        PlanningAssumptionReviewResult result =
                calculator.review(UUID.randomUUID(), LocalDate.of(2026, 9, 10), List.of());

        assertThat(result.rows()).isEmpty();
        assertThat(result.totalCount()).isZero();
        assertThat(result.needsAttentionCount()).isZero();
        assertThat(result.countsByReviewStatus().values()).allMatch(count -> count == 0);
        assertThat(result.countsByEffectiveStatus().values()).allMatch(count -> count == 0);
    }

    @Test
    void identicalInputsProduceIdenticalResults() {
        LocalDate asOf = LocalDate.of(2026, 9, 10);
        UUID id = UUID.randomUUID();
        List<PlanningAssumptionReviewSourceRecord> sources =
                List.of(assumption(id, "Stable", LocalDate.of(2026, 9, 9), LocalDate.of(2026, 1, 1), null));

        PlanningAssumptionReviewResult first = calculator.review(UUID.randomUUID(), asOf, sources);
        PlanningAssumptionReviewResult second = calculator.review(first.householdId(), asOf, sources);

        assertThat(second).isEqualTo(first);
    }

    @Test
    void doesNotCopyAnyAssumptionValueOrNoteIntoTheResult() {
        // PlanningAssumptionReviewSourceRecord itself carries no value/notes field,
        // so this is a compile-time guarantee; this test documents that invariant.
        LocalDate asOf = LocalDate.of(2026, 9, 10);
        PlanningAssumptionReviewRow row = calculator
                .review(UUID.randomUUID(), asOf,
                        List.of(assumption(UUID.randomUUID(), "Cash", asOf, LocalDate.of(2026, 1, 1), null)))
                .rows().get(0);

        assertThat(row.toString()).doesNotContain("value=").doesNotContain("notes=");
    }

    @Test
    void rejectsNullHouseholdId() {
        assertThatThrownBy(() -> calculator.review(null, LocalDate.now(), List.of()))
                .isInstanceOf(InvalidPlanningAssumptionReviewInputException.class)
                .hasMessageContaining("householdId");
    }

    @Test
    void rejectsNullAsOf() {
        assertThatThrownBy(() -> calculator.review(UUID.randomUUID(), null, List.of()))
                .isInstanceOf(InvalidPlanningAssumptionReviewInputException.class)
                .hasMessageContaining("asOf");
    }

    @Test
    void rejectsNullSourceRecordsList() {
        assertThatThrownBy(() -> calculator.review(UUID.randomUUID(), LocalDate.now(), null))
                .isInstanceOf(InvalidPlanningAssumptionReviewInputException.class)
                .hasMessageContaining("sourceRecords");
    }
}
