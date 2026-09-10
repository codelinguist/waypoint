package com.waypoint.planning.futurevalue.web.dto;

import com.waypoint.planning.futurevalue.FutureValueRow;
import java.math.BigDecimal;

public record FutureValueRowResponse(
        int month,
        BigDecimal openingBalance,
        BigDecimal growth,
        BigDecimal contribution,
        BigDecimal closingBalance
) {

    public static FutureValueRowResponse from(FutureValueRow row) {
        return new FutureValueRowResponse(
                row.month(),
                row.openingBalance(),
                row.growth(),
                row.contribution(),
                row.closingBalance()
        );
    }
}
