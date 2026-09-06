package com.waypoint.review.freshness;

/**
 * Classifies a source record's age (reviewDate - sourceDate) against the
 * caller-supplied {@code maxAgeDays} threshold. {@code STALE} means the age
 * exceeds the threshold, {@code CURRENT} means it is within the threshold
 * (inclusive, non-negative), and {@code FUTURE_DATED} means the source date
 * is after the supplied reviewDate.
 */
public enum FreshnessClassification {
    CURRENT,
    STALE,
    FUTURE_DATED
}
