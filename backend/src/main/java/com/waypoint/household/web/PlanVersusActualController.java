package com.waypoint.household.web;

import com.waypoint.household.PlanVersusActualAnalysis;
import com.waypoint.household.PlanVersusActualService;
import com.waypoint.household.web.dto.PlanVersusActualRequest;
import com.waypoint.household.web.dto.PlanVersusActualResponse;
import com.waypoint.household.web.dto.PlannedCurrencyTotalsRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/households/{householdId}/financial-snapshots/{snapshotId}/plan-comparison")
public class PlanVersusActualController {

    private final PlanVersusActualService planVersusActualService;

    public PlanVersusActualController(PlanVersusActualService planVersusActualService) {
        this.planVersusActualService = planVersusActualService;
    }

    @PostMapping
    public ResponseEntity<PlanVersusActualResponse> analyze(
            @PathVariable UUID householdId,
            @PathVariable UUID snapshotId,
            @Valid @RequestBody PlanVersusActualRequest request
    ) {
        PlanVersusActualAnalysis analysis = planVersusActualService.analyze(
                householdId,
                snapshotId,
                request.plannedMeasures().stream().map(PlannedCurrencyTotalsRequest::toDomain).toList()
        );
        return ResponseEntity.ok(PlanVersusActualResponse.from(analysis));
    }
}
