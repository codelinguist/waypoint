package com.waypoint.planning.runway;

/**
 * Distinguishes a finite constant-input runway from the case where the
 * supplied income already covers the supplied expenses.
 */
public enum RunwayStatus {
    FINITE,
    NO_SHORTFALL
}
