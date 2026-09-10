package com.waypoint.planning.multigoalfunding;

/**
 * Whether the household's available monthly budget covers the sum of every
 * goal's required monthly contribution under this model's zero-growth
 * convention.
 */
public enum MultiGoalFundingStatus {
    FITS,
    SHORTFALL
}
