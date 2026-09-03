package com.waypoint.household;

import java.util.UUID;

public class FinancialSnapshotNotFoundException extends RuntimeException {

    public FinancialSnapshotNotFoundException(UUID snapshotId) {
        super("Financial snapshot not found: " + snapshotId);
    }
}
