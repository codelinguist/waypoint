package com.waypoint.planning.runway.web.dto;

import com.waypoint.planning.runway.EmergencyFundRunwayResult;
import com.waypoint.planning.runway.RunwayStatus;
import java.math.BigDecimal;
import java.math.BigInteger;

public record EmergencyFundRunwayResponse(
        String currency,
        BigDecimal availableReserve,
        BigDecimal monthlyExpenses,
        BigDecimal monthlyNetIncome,
        BigDecimal monthlyShortfall,
        RunwayStatus status,
        BigDecimal runwayMonths,
        BigInteger fullMonthsCovered,
        String modelNote
) {

    private static final String CONSTANT_INPUT_NOTE =
            "Constant-input estimate computed only from the supplied reserve, expenses, and income; it "
            + "excludes any change in income, spending, interest, inflation, or timing within a month.";
    private static final String NO_SHORTFALL_NOTE =
            " NO_SHORTFALL describes only these supplied constant inputs, not a guarantee that income will "
            + "continue to cover expenses.";

    public static EmergencyFundRunwayResponse from(EmergencyFundRunwayResult result) {
        String modelNote = CONSTANT_INPUT_NOTE
                + (result.status() == RunwayStatus.NO_SHORTFALL ? NO_SHORTFALL_NOTE : "");
        return new EmergencyFundRunwayResponse(
                result.currency(),
                result.availableReserve(),
                result.monthlyExpenses(),
                result.monthlyNetIncome(),
                result.monthlyShortfall(),
                result.status(),
                result.runwayMonths(),
                result.fullMonthsCovered(),
                modelNote
        );
    }
}
