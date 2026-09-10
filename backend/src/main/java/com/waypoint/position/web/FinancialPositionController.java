package com.waypoint.position.web;

import com.waypoint.position.FinancialPositionResult;
import com.waypoint.position.FinancialPositionService;
import com.waypoint.position.web.dto.FinancialPositionResponse;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only current-position endpoint. {@code HouseholdNotFoundException} (404)
 * and malformed-UUID path variables (400) are already handled by the shared
 * {@code ApiExceptionHandler}, so this controller needs no feature-scoped
 * exception handler of its own.
 */
@RestController
@RequestMapping("/api/households/{householdId}/financial-position")
public class FinancialPositionController {

    private final FinancialPositionService financialPositionService;

    public FinancialPositionController(FinancialPositionService financialPositionService) {
        this.financialPositionService = financialPositionService;
    }

    @GetMapping
    public ResponseEntity<FinancialPositionResponse> getCurrentFinancialPosition(@PathVariable UUID householdId) {
        FinancialPositionResult result = financialPositionService.getCurrentFinancialPosition(householdId);
        return ResponseEntity.ok(FinancialPositionResponse.from(result));
    }
}
