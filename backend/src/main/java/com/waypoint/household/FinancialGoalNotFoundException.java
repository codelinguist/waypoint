package com.waypoint.household;

import java.util.UUID;

public class FinancialGoalNotFoundException extends RuntimeException {

    public FinancialGoalNotFoundException(UUID goalId) {
        super("Financial goal not found: " + goalId);
    }
}
