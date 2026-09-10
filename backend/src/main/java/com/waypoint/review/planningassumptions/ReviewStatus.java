package com.waypoint.review.planningassumptions;

/**
 * Classifies an assumption's {@code reviewDate} against the caller-supplied
 * {@code asOf} date. {@code OVERDUE} means the review date has passed,
 * {@code DUE_TODAY} means it equals {@code asOf}, and {@code UPCOMING} means
 * it is still ahead.
 */
public enum ReviewStatus {
    OVERDUE,
    DUE_TODAY,
    UPCOMING
}
