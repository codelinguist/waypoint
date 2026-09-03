package com.waypoint.household.web;

import com.waypoint.household.IncomeStream;
import com.waypoint.household.IncomeStreamService;
import com.waypoint.household.web.dto.CreateIncomeStreamRequest;
import com.waypoint.household.web.dto.IncomeStreamResponse;
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
@RequestMapping("/api/households/{householdId}/income-streams")
public class IncomeStreamController {

    private final IncomeStreamService incomeStreamService;

    public IncomeStreamController(IncomeStreamService incomeStreamService) {
        this.incomeStreamService = incomeStreamService;
    }

    @PostMapping
    public ResponseEntity<IncomeStreamResponse> createIncomeStream(
            @PathVariable UUID householdId,
            @Valid @RequestBody CreateIncomeStreamRequest request
    ) {
        IncomeStream incomeStream = incomeStreamService.createIncomeStream(
                householdId,
                request.name(),
                request.incomeType(),
                request.amount(),
                request.frequency(),
                request.currency(),
                request.compensationClassification(),
                request.certainty(),
                request.startDate(),
                request.endDate()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(IncomeStreamResponse.from(incomeStream));
    }

    @GetMapping("/{incomeStreamId}")
    public ResponseEntity<IncomeStreamResponse> getIncomeStream(
            @PathVariable UUID householdId,
            @PathVariable UUID incomeStreamId
    ) {
        IncomeStream incomeStream = incomeStreamService.getIncomeStream(householdId, incomeStreamId);
        return ResponseEntity.ok(IncomeStreamResponse.from(incomeStream));
    }

    @GetMapping
    public ResponseEntity<List<IncomeStreamResponse>> listIncomeStreams(@PathVariable UUID householdId) {
        List<IncomeStreamResponse> incomeStreams = incomeStreamService.listIncomeStreams(householdId).stream()
                .map(IncomeStreamResponse::from)
                .toList();
        return ResponseEntity.ok(incomeStreams);
    }
}
