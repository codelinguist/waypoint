package com.waypoint.review.freshness;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.stereotype.Service;

/**
 * Pure, stateless classification of present asset/liability source rows
 * against an explicit, caller-supplied review date and age threshold.
 * Callable directly, independently of HTTP and persistence; it enforces its
 * own input invariants rather than trusting transport-layer validation
 * alone, so a direct domain call rejects an invalid {@code reviewDate} or
 * {@code maxAgeDays} the same way a request does.
 *
 * <p>{@code ageDays} is computed with {@link ChronoUnit#DAYS} on the two
 * supplied {@link LocalDate} values, which is exact calendar-day arithmetic
 * (correct across leap years and month/year boundaries) rather than any
 * fixed-length-month or -year approximation.
 */
@Service
public class FinancialDataFreshnessCalculator {

    public static final int MIN_MAX_AGE_DAYS = 0;
    public static final int MAX_MAX_AGE_DAYS = 36500;

    public FinancialDataFreshnessResult review(
            UUID householdId,
            LocalDate reviewDate,
            int maxAgeDays,
            List<FreshnessSourceRecord> sourceRecords
    ) {
        if (householdId == null) {
            throw new InvalidFreshnessReviewInputException("householdId must not be null");
        }
        if (reviewDate == null) {
            throw new InvalidFreshnessReviewInputException("reviewDate must not be null");
        }
        if (maxAgeDays < MIN_MAX_AGE_DAYS || maxAgeDays > MAX_MAX_AGE_DAYS) {
            throw new InvalidFreshnessReviewInputException(
                    "maxAgeDays must be between " + MIN_MAX_AGE_DAYS + " and " + MAX_MAX_AGE_DAYS);
        }
        if (sourceRecords == null) {
            throw new InvalidFreshnessReviewInputException("sourceRecords must not be null");
        }

        List<FreshnessRecord> records = sourceRecords.stream()
                .map(source -> classify(reviewDate, maxAgeDays, source))
                .sorted(Comparator.comparing(FreshnessRecord::recordKind)
                        .thenComparing(FreshnessRecord::recordId))
                .toList();

        return new FinancialDataFreshnessResult(
                householdId,
                reviewDate,
                maxAgeDays,
                records,
                countBy(records, FreshnessRecord::recordKind, FreshnessRecordKind.class),
                countBy(records, FreshnessRecord::classification, FreshnessClassification.class));
    }

    private FreshnessRecord classify(LocalDate reviewDate, int maxAgeDays, FreshnessSourceRecord source) {
        if (source == null) {
            throw new InvalidFreshnessReviewInputException("sourceRecords must not contain null entries");
        }
        if (source.recordId() == null) {
            throw new InvalidFreshnessReviewInputException("recordId must not be null");
        }
        if (source.sourceDate() == null) {
            throw new InvalidFreshnessReviewInputException("sourceDate must not be null");
        }

        long ageDays = ChronoUnit.DAYS.between(source.sourceDate(), reviewDate);
        FreshnessClassification classification;
        if (ageDays < 0) {
            classification = FreshnessClassification.FUTURE_DATED;
        } else if (ageDays <= maxAgeDays) {
            classification = FreshnessClassification.CURRENT;
        } else {
            classification = FreshnessClassification.STALE;
        }

        return new FreshnessRecord(
                source.recordId(),
                source.recordKind(),
                source.name(),
                source.currency(),
                source.sourceType(),
                source.sourceDate(),
                ageDays,
                classification);
    }

    private <K extends Enum<K>> Map<K, Integer> countBy(
            List<FreshnessRecord> records, Function<FreshnessRecord, K> keyFn, Class<K> keyType
    ) {
        Map<K, Integer> counts = new EnumMap<>(keyType);
        for (K key : keyType.getEnumConstants()) {
            counts.put(key, 0);
        }
        for (FreshnessRecord record : records) {
            counts.merge(keyFn.apply(record), 1, Integer::sum);
        }
        return counts;
    }
}
