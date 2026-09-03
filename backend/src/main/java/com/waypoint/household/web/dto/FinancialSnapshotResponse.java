package com.waypoint.household.web.dto;

import com.waypoint.household.FinancialSnapshotDetail;
import com.waypoint.household.SourceType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record FinancialSnapshotResponse(
        UUID id,
        UUID householdId,
        LocalDate asOfDate,
        Instant capturedAt,
        SourceType sourceType,
        List<SnapshotAssetLineItemResponse> assetLineItems,
        List<SnapshotLiabilityLineItemResponse> liabilityLineItems,
        List<CurrencyTotalsResponse> totalsByCurrency
) {

    public static FinancialSnapshotResponse from(FinancialSnapshotDetail detail) {
        return new FinancialSnapshotResponse(
                detail.snapshot().getId(),
                detail.snapshot().getHousehold().getId(),
                detail.snapshot().getAsOfDate(),
                detail.snapshot().getCapturedAt(),
                detail.snapshot().getSourceType(),
                detail.assetLineItems().stream().map(SnapshotAssetLineItemResponse::from).toList(),
                detail.liabilityLineItems().stream().map(SnapshotLiabilityLineItemResponse::from).toList(),
                detail.totalsByCurrency().stream().map(CurrencyTotalsResponse::from).toList()
        );
    }
}
