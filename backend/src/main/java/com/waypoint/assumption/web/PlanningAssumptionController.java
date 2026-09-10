package com.waypoint.assumption.web;

import com.waypoint.assumption.PlanningAssumption;
import com.waypoint.assumption.PlanningAssumptionService;
import com.waypoint.assumption.web.dto.CreatePlanningAssumptionRequest;
import com.waypoint.assumption.web.dto.PlanningAssumptionResponse;
import com.waypoint.assumption.web.dto.SupersedePlanningAssumptionRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
            @Valid @RequestBody CreatePlanningAssumptionRequest request
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
            @RequestParam(defaultValue = "false") boolean activeOnly,
            @RequestParam(required = false) LocalDate asOf
    ) {
        List<PlanningAssumptionResponse> assumptions = planningAssumptionService
                .listAssumptions(householdId, activeOnly, asOf).stream()
                .map(PlanningAssumptionResponse::from)
                .toList();
        return ResponseEntity.ok(assumptions);
    }

    @PostMapping("/{assumptionId}/supersede")
    public ResponseEntity<PlanningAssumptionResponse> supersedeAssumption(
            @PathVariable UUID householdId,
            @PathVariable UUID assumptionId,
            @Valid @RequestBody SupersedePlanningAssumptionRequest request
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
