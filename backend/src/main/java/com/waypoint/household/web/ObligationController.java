package com.waypoint.household.web;

import com.waypoint.household.Obligation;
import com.waypoint.household.ObligationService;
import com.waypoint.household.web.dto.CreateObligationRequest;
import com.waypoint.household.web.dto.ObligationResponse;
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
@RequestMapping("/api/households/{householdId}/obligations")
public class ObligationController {

    private final ObligationService obligationService;

    public ObligationController(ObligationService obligationService) {
        this.obligationService = obligationService;
    }

    @PostMapping
    public ResponseEntity<ObligationResponse> createObligation(
            @PathVariable UUID householdId,
            @Valid @RequestBody CreateObligationRequest request
    ) {
        Obligation obligation = obligationService.createObligation(
                householdId,
                request.name(),
                request.obligationType(),
                request.amount(),
                request.frequency(),
                request.currency(),
                request.startDate(),
                request.endDate()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ObligationResponse.from(obligation));
    }

    @GetMapping("/{obligationId}")
    public ResponseEntity<ObligationResponse> getObligation(
            @PathVariable UUID householdId,
            @PathVariable UUID obligationId
    ) {
        Obligation obligation = obligationService.getObligation(householdId, obligationId);
        return ResponseEntity.ok(ObligationResponse.from(obligation));
    }

    @GetMapping
    public ResponseEntity<List<ObligationResponse>> listObligations(@PathVariable UUID householdId) {
        List<ObligationResponse> obligations = obligationService.listObligations(householdId).stream()
                .map(ObligationResponse::from)
                .toList();
        return ResponseEntity.ok(obligations);
    }
}
