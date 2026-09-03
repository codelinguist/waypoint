package com.waypoint.household.web;

import com.waypoint.household.FinancialSnapshotDetail;
import com.waypoint.household.FinancialSnapshotService;
import com.waypoint.household.web.dto.CreateFinancialSnapshotRequest;
import com.waypoint.household.web.dto.FinancialSnapshotResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/households/{householdId}/financial-snapshots")
public class FinancialSnapshotController {

    private final FinancialSnapshotService financialSnapshotService;

    public FinancialSnapshotController(FinancialSnapshotService financialSnapshotService) {
        this.financialSnapshotService = financialSnapshotService;
    }

    @PostMapping
    public ResponseEntity<FinancialSnapshotResponse> createSnapshot(
            @PathVariable UUID householdId,
            @Valid @RequestBody CreateFinancialSnapshotRequest request
    ) {
        FinancialSnapshotDetail detail = financialSnapshotService.createSnapshot(householdId, request.asOfDate());
        return ResponseEntity.status(HttpStatus.CREATED).body(FinancialSnapshotResponse.from(detail));
    }

    @GetMapping("/{snapshotId}")
    public ResponseEntity<FinancialSnapshotResponse> getSnapshot(
            @PathVariable UUID householdId,
            @PathVariable UUID snapshotId
    ) {
        FinancialSnapshotDetail detail = financialSnapshotService.getSnapshot(householdId, snapshotId);
        return ResponseEntity.ok(FinancialSnapshotResponse.from(detail));
    }

    @GetMapping
    public ResponseEntity<List<FinancialSnapshotResponse>> listSnapshots(@PathVariable UUID householdId) {
        List<FinancialSnapshotResponse> snapshots = financialSnapshotService.listSnapshots(householdId).stream()
                .map(FinancialSnapshotResponse::from)
                .toList();
        return ResponseEntity.ok(snapshots);
    }
}
