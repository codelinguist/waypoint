package com.waypoint.planning.multigoalfunding.web;

import com.waypoint.planning.multigoalfunding.GoalFundingInput;
import com.waypoint.planning.multigoalfunding.MultiGoalFundingCheckCalculator;
import com.waypoint.planning.multigoalfunding.MultiGoalFundingCheckResult;
import com.waypoint.planning.multigoalfunding.web.dto.GoalFundingInputRequest;
import com.waypoint.planning.multigoalfunding.web.dto.MultiGoalFundingCheckRequest;
import com.waypoint.planning.multigoalfunding.web.dto.MultiGoalFundingCheckResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stateless multiple-goal funding check over explicit, caller-supplied
 * inputs. Reads no household data and accepts no household or entity
 * identifier; nothing is persisted.
 */
@RestController
@RequestMapping("/api/planning/multi-goal-funding-check")
public class MultiGoalFundingCheckController {

    private final MultiGoalFundingCheckCalculator multiGoalFundingCheckCalculator;

    public MultiGoalFundingCheckController(MultiGoalFundingCheckCalculator multiGoalFundingCheckCalculator) {
        this.multiGoalFundingCheckCalculator = multiGoalFundingCheckCalculator;
    }

    @PostMapping
    public ResponseEntity<MultiGoalFundingCheckResponse> calculate(
            @Valid @RequestBody MultiGoalFundingCheckRequest request
    ) {
        List<GoalFundingInput> goals = request.goals().stream()
                .map(MultiGoalFundingCheckController::toGoalFundingInput)
                .toList();
        MultiGoalFundingCheckResult result = multiGoalFundingCheckCalculator.calculate(
                request.currency(), request.availableMonthlyBudget(), goals);
        return ResponseEntity.ok(MultiGoalFundingCheckResponse.from(result));
    }

    private static GoalFundingInput toGoalFundingInput(GoalFundingInputRequest goal) {
        if (goal == null) {
            return null;
        }
        return new GoalFundingInput(
                goal.reference(), goal.targetAmount(), goal.currentAmount(), goal.contributionMonths());
    }
}
