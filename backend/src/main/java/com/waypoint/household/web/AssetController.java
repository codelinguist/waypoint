package com.waypoint.household.web;

import com.waypoint.household.Asset;
import com.waypoint.household.AssetService;
import com.waypoint.household.web.dto.AssetResponse;
import com.waypoint.household.web.dto.AssetValuationHistoryResponse;
import com.waypoint.household.web.dto.AssetValuationStateResponse;
import com.waypoint.household.web.dto.CreateAssetRequest;
import com.waypoint.household.web.dto.UpdateAssetValuationRequest;
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
@RequestMapping("/api/households/{householdId}/assets")
public class AssetController {

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    @PostMapping
    public ResponseEntity<AssetResponse> createAsset(
            @PathVariable UUID householdId,
            @Valid @RequestBody CreateAssetRequest request
    ) {
        Asset asset = assetService.createAsset(
                householdId,
                request.name(),
                request.assetType(),
                request.estimatedValue(),
                request.planningValue(),
                request.currency(),
                request.valuedAt(),
                request.liquidity()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(AssetResponse.from(asset));
    }

    @GetMapping("/{assetId}")
    public ResponseEntity<AssetResponse> getAsset(@PathVariable UUID householdId, @PathVariable UUID assetId) {
        Asset asset = assetService.getAsset(householdId, assetId);
        return ResponseEntity.ok(AssetResponse.from(asset));
    }

    @GetMapping
    public ResponseEntity<List<AssetResponse>> listAssets(@PathVariable UUID householdId) {
        List<AssetResponse> assets = assetService.listAssets(householdId).stream()
                .map(AssetResponse::from)
                .toList();
        return ResponseEntity.ok(assets);
    }

    @GetMapping("/{assetId}/valuations")
    public ResponseEntity<AssetValuationStateResponse> getCurrentValuation(
            @PathVariable UUID householdId,
            @PathVariable UUID assetId
    ) {
        Asset asset = assetService.getAsset(householdId, assetId);
        return ResponseEntity.ok(AssetValuationStateResponse.from(asset));
    }

    @PostMapping("/{assetId}/valuations")
    public ResponseEntity<AssetValuationStateResponse> updateValuation(
            @PathVariable UUID householdId,
            @PathVariable UUID assetId,
            @Valid @RequestBody UpdateAssetValuationRequest request
    ) {
        Asset asset = assetService.updateValuation(
                householdId,
                assetId,
                request.estimatedValue(),
                request.planningValue(),
                request.valuedAt(),
                request.reason(),
                request.expectedRevision()
        );
        return ResponseEntity.ok(AssetValuationStateResponse.from(asset));
    }

    @GetMapping("/{assetId}/valuations/history")
    public ResponseEntity<List<AssetValuationHistoryResponse>> getValuationHistory(
            @PathVariable UUID householdId,
            @PathVariable UUID assetId
    ) {
        List<AssetValuationHistoryResponse> history = assetService.listValuationHistory(householdId, assetId)
                .stream()
                .map(AssetValuationHistoryResponse::from)
                .toList();
        return ResponseEntity.ok(history);
    }
}
