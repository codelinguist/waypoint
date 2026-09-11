package com.waypoint.review.planningassumptions;

/**
 * Classifies an assumption's effective window against the caller-supplied
 * {@code asOf} date. {@code NOT_YET_EFFECTIVE} means {@code asOf} precedes
 * {@code effectiveFrom}; {@code EXPIRED} means a non-null
 * {@code effectiveUntil} precedes {@code asOf} (the end date is inclusive,
 * so {@code effectiveUntil == asOf} is still {@code EFFECTIVE}); otherwise
 * {@code EFFECTIVE}, including every open-ended (null {@code effectiveUntil})
 * version that has started.
 */
public enum EffectiveStatus {
    NOT_YET_EFFECTIVE,
    EFFECTIVE,
    EXPIRED
}
