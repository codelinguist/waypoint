package com.waypoint.position.web.dto;

import com.waypoint.position.FinancialPositionResult;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FinancialPositionResponse(
        UUID householdId,
        String householdName,
        String baseCurrency,
        Instant retrievedAt,
        List<PositionAssetResponse> assets,
        List<PositionLiabilityResponse> liabilities,
        List<PositionCurrencyTotalsResponse> totalsByCurrency
) {

    public static FinancialPositionResponse from(FinancialPositionResult result) {
        return new FinancialPositionResponse(
                result.household().getId(),
                result.household().getName(),
                result.household().getBaseCurrency(),
                result.retrievedAt(),
                result.assets().stream().map(PositionAssetResponse::from).toList(),
                result.liabilities().stream().map(PositionLiabilityResponse::from).toList(),
                result.totalsByCurrency().stream().map(PositionCurrencyTotalsResponse::from).toList()
        );
    }
}
