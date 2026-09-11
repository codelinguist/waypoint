package com.waypoint.review.planningassumptions;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.stereotype.Service;

/**
 * Pure, stateless classification of present unsuperseded planning-assumption
 * source rows against an explicit, caller-supplied {@code asOf} date.
 * Callable directly, independently of HTTP and persistence; it enforces its
 * own input invariants rather than trusting transport-layer validation
 * alone, so a direct domain call rejects a null {@code asOf} the same way a
 * request does. It never reads the server clock.
 *
 * <p>This reviews the household's current unsuperseded assumption versions
 * only; it does not reconstruct which beliefs were current as of a past
 * {@code asOf} date. Superseded versions must already be excluded from
 * {@code sourceRecords} by the caller.
 */
@Service
public class PlanningAssumptionReviewCalculator {

    public PlanningAssumptionReviewResult review(
            UUID householdId,
            LocalDate asOf,
            List<PlanningAssumptionReviewSourceRecord> sourceRecords
    ) {
        if (householdId == null) {
            throw new InvalidPlanningAssumptionReviewInputException("householdId must not be null");
        }
        if (asOf == null) {
            throw new InvalidPlanningAssumptionReviewInputException("asOf must not be null");
        }
        if (sourceRecords == null) {
            throw new InvalidPlanningAssumptionReviewInputException("sourceRecords must not be null");
        }

        List<PlanningAssumptionReviewRow> rows = sourceRecords.stream()
                .map(source -> classify(asOf, source))
                .sorted(Comparator.comparing(PlanningAssumptionReviewRow::reviewDate)
                        .thenComparing(PlanningAssumptionReviewRow::assumptionId))
                .toList();

        int needsAttentionCount = (int) rows.stream().filter(PlanningAssumptionReviewRow::needsAttention).count();

        return new PlanningAssumptionReviewResult(
                householdId,
                asOf,
                rows,
                rows.size(),
                needsAttentionCount,
                countBy(rows, PlanningAssumptionReviewRow::reviewStatus, ReviewStatus.class),
                countBy(rows, PlanningAssumptionReviewRow::effectiveStatus, EffectiveStatus.class));
    }

    private PlanningAssumptionReviewRow classify(LocalDate asOf, PlanningAssumptionReviewSourceRecord source) {
        if (source == null) {
            throw new InvalidPlanningAssumptionReviewInputException("sourceRecords must not contain null entries");
        }
        if (source.assumptionId() == null) {
            throw new InvalidPlanningAssumptionReviewInputException("assumptionId must not be null");
        }
        if (source.reviewDate() == null) {
            throw new InvalidPlanningAssumptionReviewInputException("reviewDate must not be null");
        }
        if (source.effectiveFrom() == null) {
            throw new InvalidPlanningAssumptionReviewInputException("effectiveFrom must not be null");
        }

        ReviewStatus reviewStatus;
        if (source.reviewDate().isBefore(asOf)) {
            reviewStatus = ReviewStatus.OVERDUE;
        } else if (source.reviewDate().isEqual(asOf)) {
            reviewStatus = ReviewStatus.DUE_TODAY;
        } else {
            reviewStatus = ReviewStatus.UPCOMING;
        }

        EffectiveStatus effectiveStatus;
        if (asOf.isBefore(source.effectiveFrom())) {
            effectiveStatus = EffectiveStatus.NOT_YET_EFFECTIVE;
        } else if (source.effectiveUntil() != null && source.effectiveUntil().isBefore(asOf)) {
            effectiveStatus = EffectiveStatus.EXPIRED;
        } else {
            effectiveStatus = EffectiveStatus.EFFECTIVE;
        }

        boolean needsAttention = reviewStatus == ReviewStatus.OVERDUE
                || reviewStatus == ReviewStatus.DUE_TODAY
                || effectiveStatus == EffectiveStatus.EXPIRED;

        return new PlanningAssumptionReviewRow(
                source.assumptionId(),
                source.name(),
                source.reviewDate(),
                source.effectiveFrom(),
                source.effectiveUntil(),
                source.sourceType(),
                reviewStatus,
                effectiveStatus,
                needsAttention);
    }

    private <K extends Enum<K>> Map<K, Integer> countBy(
            List<PlanningAssumptionReviewRow> rows, Function<PlanningAssumptionReviewRow, K> keyFn, Class<K> keyType
    ) {
        Map<K, Integer> counts = new EnumMap<>(keyType);
        for (K key : keyType.getEnumConstants()) {
            counts.put(key, 0);
        }
        for (PlanningAssumptionReviewRow row : rows) {
            counts.merge(keyFn.apply(row), 1, Integer::sum);
        }
        return counts;
    }
}
