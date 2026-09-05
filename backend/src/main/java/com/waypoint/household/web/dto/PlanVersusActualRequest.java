package com.waypoint.household.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record PlanVersusActualRequest(
        @NotEmpty(message = "plannedMeasures must not be empty")
        @Valid
        List<PlannedCurrencyTotalsRequest> plannedMeasures
) {
}
