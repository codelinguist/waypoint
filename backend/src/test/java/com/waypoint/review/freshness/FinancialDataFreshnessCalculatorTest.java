package com.waypoint.review.freshness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.waypoint.household.SourceType;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FinancialDataFreshnessCalculatorTest {

    private final FinancialDataFreshnessCalculator calculator = new FinancialDataFreshnessCalculator();

    private static FreshnessSourceRecord asset(UUID id, String name, LocalDate sourceDate) {
        return new FreshnessSourceRecord(id, FreshnessRecordKind.ASSET, name, "PHP", SourceType.MANUAL_ENTRY, sourceDate);
    }

    private static FreshnessSourceRecord liability(UUID id, String name, LocalDate sourceDate) {
        return new FreshnessSourceRecord(id, FreshnessRecordKind.LIABILITY, name, "PHP", SourceType.MANUAL_ENTRY, sourceDate);
    }

    @Test
    void classifiesExactlyAtThresholdAsCurrent() {
        LocalDate reviewDate = LocalDate.of(2026, 9, 6);
        FreshnessSourceRecord source = asset(UUID.randomUUID(), "Cash", LocalDate.of(2026, 8, 7));

        FinancialDataFreshnessResult result = calculator.review(UUID.randomUUID(), reviewDate, 30, List.of(source));

        FreshnessRecord record = result.records().get(0);
        assertThat(record.ageDays()).isEqualTo(30);
        assertThat(record.classification()).isEqualTo(FreshnessClassification.CURRENT);
    }

    @Test
    void classifiesOneDayPastThresholdAsStale() {
        LocalDate reviewDate = LocalDate.of(2026, 9, 6);
        FreshnessSourceRecord source = asset(UUID.randomUUID(), "Cash", LocalDate.of(2026, 8, 6));

        FinancialDataFreshnessResult result = calculator.review(UUID.randomUUID(), reviewDate, 30, List.of(source));

        FreshnessRecord record = result.records().get(0);
        assertThat(record.ageDays()).isEqualTo(31);
        assertThat(record.classification()).isEqualTo(FreshnessClassification.STALE);
    }

    @Test
    void classifiesSourceDateAfterReviewDateAsFutureDated() {
        LocalDate reviewDate = LocalDate.of(2026, 9, 6);
        FreshnessSourceRecord source = asset(UUID.randomUUID(), "Cash", LocalDate.of(2026, 9, 7));

        FinancialDataFreshnessResult result = calculator.review(UUID.randomUUID(), reviewDate, 30, List.of(source));

        FreshnessRecord record = result.records().get(0);
        assertThat(record.ageDays()).isEqualTo(-1);
        assertThat(record.classification()).isEqualTo(FreshnessClassification.FUTURE_DATED);
    }

    @Test
    void zeroThresholdMarksSameDayCurrentAndEarlierStale() {
        LocalDate reviewDate = LocalDate.of(2026, 9, 6);
        FreshnessSourceRecord sameDay = asset(UUID.randomUUID(), "Same day", reviewDate);
        FreshnessSourceRecord earlier = liability(UUID.randomUUID(), "Earlier", reviewDate.minusDays(1));

        FinancialDataFreshnessResult result =
                calculator.review(UUID.randomUUID(), reviewDate, 0, List.of(sameDay, earlier));

        FreshnessRecord sameDayRecord = result.records().stream()
                .filter(r -> r.recordKind() == FreshnessRecordKind.ASSET).findFirst().orElseThrow();
        FreshnessRecord earlierRecord = result.records().stream()
                .filter(r -> r.recordKind() == FreshnessRecordKind.LIABILITY).findFirst().orElseThrow();

        assertThat(sameDayRecord.ageDays()).isZero();
        assertThat(sameDayRecord.classification()).isEqualTo(FreshnessClassification.CURRENT);
        assertThat(earlierRecord.ageDays()).isEqualTo(1);
        assertThat(earlierRecord.classification()).isEqualTo(FreshnessClassification.STALE);
    }

    @Test
    void usesExactCalendarArithmeticAcrossALeapYearBoundary() {
        // 2024 is a leap year: Feb 29 exists, so Feb 29 -> Mar 1 is exactly 1 day.
        LocalDate reviewDate = LocalDate.of(2024, 3, 1);
        FreshnessSourceRecord source = asset(UUID.randomUUID(), "Cash", LocalDate.of(2024, 2, 29));

        FinancialDataFreshnessResult result = calculator.review(UUID.randomUUID(), reviewDate, 30, List.of(source));

        assertThat(result.records().get(0).ageDays()).isEqualTo(1);
    }

    @Test
    void usesExactCalendarArithmeticAcrossANonLeapYearMonthBoundary() {
        // 2023 is not a leap year: Feb has 28 days, so Feb 28 -> Mar 1 is exactly 1 day.
        LocalDate reviewDate = LocalDate.of(2023, 3, 1);
        FreshnessSourceRecord source = asset(UUID.randomUUID(), "Cash", LocalDate.of(2023, 2, 28));

        FinancialDataFreshnessResult result = calculator.review(UUID.randomUUID(), reviewDate, 30, List.of(source));

        assertThat(result.records().get(0).ageDays()).isEqualTo(1);
    }

    @Test
    void usesExactCalendarArithmeticAcrossAYearBoundary() {
        LocalDate reviewDate = LocalDate.of(2026, 1, 1);
        FreshnessSourceRecord source = asset(UUID.randomUUID(), "Cash", LocalDate.of(2025, 12, 31));

        FinancialDataFreshnessResult result = calculator.review(UUID.randomUUID(), reviewDate, 30, List.of(source));

        assertThat(result.records().get(0).ageDays()).isEqualTo(1);
    }

    @Test
    void ordersRecordsByKindThenById() {
        LocalDate reviewDate = LocalDate.of(2026, 9, 6);
        UUID assetLowId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID assetHighId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID liabilityId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        FinancialDataFreshnessResult result = calculator.review(UUID.randomUUID(), reviewDate, 30, List.of(
                liability(liabilityId, "Loan", reviewDate),
                asset(assetHighId, "Investment", reviewDate),
                asset(assetLowId, "Cash", reviewDate)));

        assertThat(result.records()).extracting(FreshnessRecord::recordId)
                .containsExactly(assetLowId, assetHighId, liabilityId);
        assertThat(result.records()).extracting(FreshnessRecord::recordKind)
                .containsExactly(
                        FreshnessRecordKind.ASSET, FreshnessRecordKind.ASSET, FreshnessRecordKind.LIABILITY);
    }

    @Test
    void countsAreBrokenDownByKindAndClassificationIncludingZeroes() {
        LocalDate reviewDate = LocalDate.of(2026, 9, 6);
        FinancialDataFreshnessResult result = calculator.review(UUID.randomUUID(), reviewDate, 30, List.of(
                asset(UUID.randomUUID(), "Current asset", reviewDate),
                liability(UUID.randomUUID(), "Stale liability", reviewDate.minusDays(31))));

        assertThat(result.countsByKind())
                .containsEntry(FreshnessRecordKind.ASSET, 1)
                .containsEntry(FreshnessRecordKind.LIABILITY, 1);
        assertThat(result.countsByClassification())
                .containsEntry(FreshnessClassification.CURRENT, 1)
                .containsEntry(FreshnessClassification.STALE, 1)
                .containsEntry(FreshnessClassification.FUTURE_DATED, 0);
    }

    @Test
    void emptySourceRecordsYieldEmptyListAndZeroCounts() {
        FinancialDataFreshnessResult result =
                calculator.review(UUID.randomUUID(), LocalDate.of(2026, 9, 6), 30, List.of());

        assertThat(result.records()).isEmpty();
        assertThat(result.countsByKind().values()).allMatch(count -> count == 0);
        assertThat(result.countsByClassification().values()).allMatch(count -> count == 0);
    }

    @Test
    void identicalInputsProduceIdenticalResults() {
        LocalDate reviewDate = LocalDate.of(2026, 9, 6);
        UUID id = UUID.randomUUID();
        List<FreshnessSourceRecord> sources = List.of(asset(id, "Cash", reviewDate.minusDays(10)));

        FinancialDataFreshnessResult first = calculator.review(UUID.randomUUID(), reviewDate, 30, sources);
        FinancialDataFreshnessResult second = calculator.review(first.householdId(), reviewDate, 30, sources);

        assertThat(second).isEqualTo(first);
    }

    @Test
    void rejectsNullHouseholdId() {
        assertThatThrownBy(() -> calculator.review(null, LocalDate.now(), 30, List.of()))
                .isInstanceOf(InvalidFreshnessReviewInputException.class)
                .hasMessageContaining("householdId");
    }

    @Test
    void rejectsNullReviewDate() {
        assertThatThrownBy(() -> calculator.review(UUID.randomUUID(), null, 30, List.of()))
                .isInstanceOf(InvalidFreshnessReviewInputException.class)
                .hasMessageContaining("reviewDate");
    }

    @Test
    void rejectsNegativeMaxAgeDays() {
        assertThatThrownBy(() -> calculator.review(UUID.randomUUID(), LocalDate.now(), -1, List.of()))
                .isInstanceOf(InvalidFreshnessReviewInputException.class)
                .hasMessageContaining("maxAgeDays");
    }

    @Test
    void rejectsMaxAgeDaysAboveUpperBound() {
        assertThatThrownBy(() -> calculator.review(UUID.randomUUID(), LocalDate.now(), 36501, List.of()))
                .isInstanceOf(InvalidFreshnessReviewInputException.class)
                .hasMessageContaining("maxAgeDays");
    }

    @Test
    void acceptsMaxAgeDaysAtTheUpperBound() {
        FinancialDataFreshnessResult result =
                calculator.review(UUID.randomUUID(), LocalDate.now(), 36500, List.of());

        assertThat(result.maxAgeDays()).isEqualTo(36500);
    }

    @Test
    void rejectsNullSourceRecordsList() {
        assertThatThrownBy(() -> calculator.review(UUID.randomUUID(), LocalDate.now(), 30, null))
                .isInstanceOf(InvalidFreshnessReviewInputException.class)
                .hasMessageContaining("sourceRecords");
    }

    @Test
    void doesNotCopyAnyFinancialAmountIntoTheResult() {
        // FreshnessSourceRecord itself carries no amount field, so this is a
        // compile-time guarantee; this test documents that invariant.
        LocalDate reviewDate = LocalDate.of(2026, 9, 6);
        FreshnessRecord record = calculator
                .review(UUID.randomUUID(), reviewDate, 30, List.of(asset(UUID.randomUUID(), "Cash", reviewDate)))
                .records().get(0);

        assertThat(record.toString())
                .doesNotContain("estimatedValue")
                .doesNotContain("planningValue")
                .doesNotContain("outstandingBalance");
    }
}
