package com.waypoint.household;

import java.util.UUID;

public class IncomeStreamNotFoundException extends RuntimeException {

    public IncomeStreamNotFoundException(UUID incomeStreamId) {
        super("Income stream not found: " + incomeStreamId);
    }
}
