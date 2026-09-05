package com.waypoint.planning.goalcontribution.web;

import com.waypoint.planning.goalcontribution.GoalContributionCalculator;
import com.waypoint.planning.goalcontribution.GoalContributionResult;
import com.waypoint.planning.goalcontribution.web.dto.GoalContributionRequest;
import com.waypoint.planning.goalcontribution.web.dto.GoalContributionResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stateless equal-monthly-goal-contribution calculation over explicit,
 * caller-supplied inputs. Reads no household data and accepts no household
 * or entity identifier; nothing is persisted.
 */
@RestController
@RequestMapping("/api/planning/goal-contribution-calculator")
public class GoalContributionController {

    private final GoalContributionCalculator goalContributionCalculator;

    public GoalContributionController(GoalContributionCalculator goalContributionCalculator) {
        this.goalContributionCalculator = goalContributionCalculator;
    }

    @PostMapping
    public ResponseEntity<GoalContributionResponse> calculate(@Valid @RequestBody GoalContributionRequest request) {
        GoalContributionResult result = goalContributionCalculator.calculate(
                request.currency(),
                request.targetAmount(),
                request.currentAmount(),
                request.contributionMonths());
        return ResponseEntity.ok(GoalContributionResponse.from(result));
    }
}
