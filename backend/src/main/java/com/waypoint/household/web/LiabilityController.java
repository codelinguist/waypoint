package com.waypoint.household.web;

import com.waypoint.household.Liability;
import com.waypoint.household.LiabilityService;
import com.waypoint.household.web.dto.CreateLiabilityRequest;
import com.waypoint.household.web.dto.LiabilityResponse;
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
@RequestMapping("/api/households/{householdId}/liabilities")
public class LiabilityController {

    private final LiabilityService liabilityService;

    public LiabilityController(LiabilityService liabilityService) {
        this.liabilityService = liabilityService;
    }

    @PostMapping
    public ResponseEntity<LiabilityResponse> createLiability(
            @PathVariable UUID householdId,
            @Valid @RequestBody CreateLiabilityRequest request
    ) {
        Liability liability = liabilityService.createLiability(
                householdId,
                request.name(),
                request.liabilityType(),
                request.outstandingBalance(),
                request.currency(),
                request.balanceAsOf()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(LiabilityResponse.from(liability));
    }

    @GetMapping("/{liabilityId}")
    public ResponseEntity<LiabilityResponse> getLiability(
            @PathVariable UUID householdId,
            @PathVariable UUID liabilityId
    ) {
        Liability liability = liabilityService.getLiability(householdId, liabilityId);
        return ResponseEntity.ok(LiabilityResponse.from(liability));
    }

    @GetMapping
    public ResponseEntity<List<LiabilityResponse>> listLiabilities(@PathVariable UUID householdId) {
        List<LiabilityResponse> liabilities = liabilityService.listLiabilities(householdId).stream()
                .map(LiabilityResponse::from)
                .toList();
        return ResponseEntity.ok(liabilities);
    }
}
