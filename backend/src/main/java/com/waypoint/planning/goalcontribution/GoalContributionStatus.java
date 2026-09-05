package com.waypoint.planning.goalcontribution;

/**
 * Distinguishes a goal that already has enough current amount from one that
 * still needs monthly contributions under this model's zero-growth
 * convention.
 */
public enum GoalContributionStatus {
    ALREADY_FUNDED,
    CONTRIBUTIONS_REQUIRED
}
