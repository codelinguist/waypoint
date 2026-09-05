package com.waypoint.planning.cashflow.web;

import com.waypoint.planning.cashflow.CashFlowProjectionCalculator;
import com.waypoint.planning.cashflow.CashFlowProjectionResult;
import com.waypoint.planning.cashflow.web.dto.CashFlowProjectionRequest;
import com.waypoint.planning.cashflow.web.dto.CashFlowProjectionResponse;
import jakarta.validation.Valid;
import java.time.YearMonth;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stateless constant-monthly-cash-flow projection over explicit, caller-supplied inputs. Reads
 * no household data and accepts no household or entity identifier; nothing is persisted.
 */
@RestController
@RequestMapping("/api/planning/cash-flow-projection")
public class CashFlowProjectionController {

    private final CashFlowProjectionCalculator cashFlowProjectionCalculator;

    public CashFlowProjectionController(CashFlowProjectionCalculator cashFlowProjectionCalculator) {
        this.cashFlowProjectionCalculator = cashFlowProjectionCalculator;
    }

    @PostMapping
    public ResponseEntity<CashFlowProjectionResponse> calculate(@Valid @RequestBody CashFlowProjectionRequest request) {
        CashFlowProjectionResult result = cashFlowProjectionCalculator.calculate(
                request.currency(),
                YearMonth.parse(request.startMonth()),
                request.startingCash(),
                request.monthlyInflow(),
                request.monthlyOutflow(),
                request.months());
        return ResponseEntity.ok(CashFlowProjectionResponse.from(result));
    }
}
