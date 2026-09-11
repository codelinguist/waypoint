package com.waypoint.household;

import java.util.UUID;

public class StaleLiabilityRevisionException extends RuntimeException {

    public StaleLiabilityRevisionException(UUID liabilityId, long expectedRevision) {
        super("Liability " + liabilityId + " balance update rejected: revision " + expectedRevision
                + " is not the current revision");
    }
}
