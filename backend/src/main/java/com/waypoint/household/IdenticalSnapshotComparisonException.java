package com.waypoint.household;

import java.util.UUID;

public class IdenticalSnapshotComparisonException extends RuntimeException {

    public IdenticalSnapshotComparisonException(UUID snapshotId) {
        super("Cannot compare a financial snapshot against itself: " + snapshotId);
    }
}
