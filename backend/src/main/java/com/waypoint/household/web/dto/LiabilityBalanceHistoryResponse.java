package com.waypoint.household.web.dto;

import com.waypoint.household.LiabilityBalanceHistory;
import com.waypoint.household.SourceType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record LiabilityBalanceHistoryResponse(
        UUID id,
        UUID liabilityId,
        UUID householdId,
        String currency,
        String previousBalance,
        LocalDate previousBalanceAsOf,
        SourceType previousSourceType,
        String newBalance,
        LocalDate newBalanceAsOf,
        SourceType newSourceType,
        String reason,
        long revision,
        Instant recordedAt
) {

    public static LiabilityBalanceHistoryResponse from(LiabilityBalanceHistory history) {
        return new LiabilityBalanceHistoryResponse(
                history.getId(),
                history.getLiability().getId(),
                history.getLiability().getHousehold().getId(),
                history.getCurrency(),
                MoneyFormat.plain(history.getPreviousBalance()),
                history.getPreviousBalanceAsOf(),
                history.getPreviousSourceType(),
                MoneyFormat.plain(history.getNewBalance()),
                history.getNewBalanceAsOf(),
                history.getNewSourceType(),
                history.getReason(),
                history.getRevision(),
                history.getRecordedAt()
        );
    }
}
