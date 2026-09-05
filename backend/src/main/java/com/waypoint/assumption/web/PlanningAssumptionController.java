package com.waypoint.assumption.web;

import com.waypoint.assumption.InvalidPlanningAssumptionException;
import com.waypoint.assumption.PlanningAssumption;
import com.waypoint.assumption.PlanningAssumptionService;
import com.waypoint.assumption.web.dto.PlanningAssumptionRequest;
import com.waypoint.assumption.web.dto.PlanningAssumptionResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Private household API for recording, retrieving, and superseding planning
 * assumptions. These are explicitly non-factual planning beliefs — this
 * controller never reads or writes canonical household financial state.
 */
@RestController
@RequestMapping("/api/households/{householdId}/assumptions")
public class PlanningAssumptionController {

    private final PlanningAssumptionService planningAssumptionService;

    public PlanningAssumptionController(PlanningAssumptionService planningAssumptionService) {
        this.planningAssumptionService = planningAssumptionService;
    }

    @PostMapping
    public ResponseEntity<PlanningAssumptionResponse> createAssumption(
            @PathVariable UUID householdId,
            @Valid @RequestBody PlanningAssumptionRequest request
    ) {
        PlanningAssumption assumption = planningAssumptionService.createAssumption(
                householdId,
                request.name(),
                request.value(),
                request.valueType(),
                request.notes(),
                request.effectiveFrom(),
                request.effectiveUntil(),
                request.reviewDate()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(PlanningAssumptionResponse.from(assumption));
    }

    @GetMapping("/{assumptionId}")
    public ResponseEntity<PlanningAssumptionResponse> getAssumption(
            @PathVariable UUID householdId,
            @PathVariable UUID assumptionId
    ) {
        PlanningAssumption assumption = planningAssumptionService.getAssumption(householdId, assumptionId);
        return ResponseEntity.ok(PlanningAssumptionResponse.from(assumption));
    }

    @GetMapping
    public ResponseEntity<List<PlanningAssumptionResponse>> listAssumptions(
            @PathVariable UUID householdId,
            @RequestParam(name = "activeOnly", defaultValue = "false") boolean activeOnly,
            @RequestParam(name = "asOf", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf
    ) {
        List<PlanningAssumption> assumptions;
        if (activeOnly) {
            if (asOf == null) {
                throw new InvalidPlanningAssumptionException("asOf is required when activeOnly=true");
            }
            assumptions = planningAssumptionService.listActiveAssumptions(householdId, asOf);
        } else {
            assumptions = planningAssumptionService.listAssumptions(householdId);
        }
        return ResponseEntity.ok(assumptions.stream().map(PlanningAssumptionResponse::from).toList());
    }

    @PostMapping("/{assumptionId}/supersede")
    public ResponseEntity<PlanningAssumptionResponse> supersedeAssumption(
            @PathVariable UUID householdId,
            @PathVariable UUID assumptionId,
            @Valid @RequestBody PlanningAssumptionRequest request
    ) {
        PlanningAssumption replacement = planningAssumptionService.supersedeAssumption(
                householdId,
                assumptionId,
                request.name(),
                request.value(),
                request.valueType(),
                request.notes(),
                request.effectiveFrom(),
                request.effectiveUntil(),
                request.reviewDate()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(PlanningAssumptionResponse.from(replacement));
    }
}
