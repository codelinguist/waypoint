package com.waypoint.household.web;

import com.waypoint.household.FinancialGoal;
import com.waypoint.household.FinancialGoalService;
import com.waypoint.household.web.dto.CreateFinancialGoalRequest;
import com.waypoint.household.web.dto.FinancialGoalResponse;
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
@RequestMapping("/api/households/{householdId}/goals")
public class FinancialGoalController {

    private final FinancialGoalService financialGoalService;

    public FinancialGoalController(FinancialGoalService financialGoalService) {
        this.financialGoalService = financialGoalService;
    }

    @PostMapping
    public ResponseEntity<FinancialGoalResponse> createGoal(
            @PathVariable UUID householdId,
            @Valid @RequestBody CreateFinancialGoalRequest request
    ) {
        FinancialGoal goal = financialGoalService.createGoal(
                householdId,
                request.name(),
                request.targetAmount(),
                request.currency(),
                request.targetDate(),
                request.priority(),
                request.currentAmount()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(FinancialGoalResponse.from(goal));
    }

    @GetMapping("/{goalId}")
    public ResponseEntity<FinancialGoalResponse> getGoal(
            @PathVariable UUID householdId,
            @PathVariable UUID goalId
    ) {
        FinancialGoal goal = financialGoalService.getGoal(householdId, goalId);
        return ResponseEntity.ok(FinancialGoalResponse.from(goal));
    }

    @GetMapping
    public ResponseEntity<List<FinancialGoalResponse>> listGoals(@PathVariable UUID householdId) {
        List<FinancialGoalResponse> goals = financialGoalService.listGoals(householdId).stream()
                .map(FinancialGoalResponse::from)
                .toList();
        return ResponseEntity.ok(goals);
    }
}
