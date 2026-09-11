package com.waypoint.household;

import java.util.UUID;

public class StaleAssetRevisionException extends RuntimeException {

    public StaleAssetRevisionException(UUID assetId, long expectedRevision) {
        super("Asset " + assetId + " revision has changed; expected revision " + expectedRevision + " is stale");
    }
}
