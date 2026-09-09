package com.waypoint.planning.futurevalue.web;

import com.waypoint.planning.futurevalue.FutureValueCalculator;
import com.waypoint.planning.futurevalue.FutureValueResult;
import com.waypoint.planning.futurevalue.web.dto.FutureValueRequest;
import com.waypoint.planning.futurevalue.web.dto.FutureValueResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stateless monthly compound-growth projection over explicit, caller-supplied inputs. Reads no
 * household data and accepts no household or entity identifier; nothing is persisted.
 */
@RestController
@RequestMapping("/api/planning/future-value")
public class FutureValueController {

    private final FutureValueCalculator futureValueCalculator;

    public FutureValueController(FutureValueCalculator futureValueCalculator) {
        this.futureValueCalculator = futureValueCalculator;
    }

    @PostMapping
    public ResponseEntity<FutureValueResponse> calculate(@Valid @RequestBody FutureValueRequest request) {
        FutureValueResult result = futureValueCalculator.calculate(
                request.currency(),
                request.startingPrincipal(),
                request.monthlyContribution(),
                request.annualRatePercentage(),
                request.projectionMonths());
        return ResponseEntity.ok(FutureValueResponse.from(result));
    }
}
