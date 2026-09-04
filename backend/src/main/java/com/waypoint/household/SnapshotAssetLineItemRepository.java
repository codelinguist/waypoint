package com.waypoint.household;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SnapshotAssetLineItemRepository extends JpaRepository<SnapshotAssetLineItem, UUID> {

    List<SnapshotAssetLineItem> findBySnapshot_IdOrderByCreatedAtAscIdAsc(UUID snapshotId);
}
