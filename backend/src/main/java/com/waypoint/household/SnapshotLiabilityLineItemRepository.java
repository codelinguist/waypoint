package com.waypoint.household;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SnapshotLiabilityLineItemRepository extends JpaRepository<SnapshotLiabilityLineItem, UUID> {

    List<SnapshotLiabilityLineItem> findBySnapshot_IdOrderByCreatedAtAscIdAsc(UUID snapshotId);
}
