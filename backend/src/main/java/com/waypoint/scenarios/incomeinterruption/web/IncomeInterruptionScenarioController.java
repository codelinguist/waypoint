package com.waypoint.scenarios.incomeinterruption.web;

import com.waypoint.scenarios.incomeinterruption.IncomeInterruptionScenarioCalculator;
import com.waypoint.scenarios.incomeinterruption.IncomeInterruptionScenarioResult;
import com.waypoint.scenarios.incomeinterruption.web.dto.IncomeInterruptionScenarioRequest;
import com.waypoint.scenarios.incomeinterruption.web.dto.IncomeInterruptionScenarioResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stateless income-interruption reserve scenario over explicit, caller-supplied inputs. Reads no
 * household data and accepts no household or entity identifier; nothing is persisted.
 */
@RestController
@RequestMapping("/api/scenarios/income-interruption")
public class IncomeInterruptionScenarioController {

    private final IncomeInterruptionScenarioCalculator incomeInterruptionScenarioCalculator;

    public IncomeInterruptionScenarioController(
            IncomeInterruptionScenarioCalculator incomeInterruptionScenarioCalculator) {
        this.incomeInterruptionScenarioCalculator = incomeInterruptionScenarioCalculator;
    }

    @PostMapping
    public ResponseEntity<IncomeInterruptionScenarioResponse> calculate(
            @Valid @RequestBody IncomeInterruptionScenarioRequest request) {
        IncomeInterruptionScenarioResult result = incomeInterruptionScenarioCalculator.calculate(
                request.currency(),
                request.openingReserve(),
                request.normalMonthlyNetIncome(),
                request.interruptedMonthlyNetIncome(),
                request.monthlyExpenses(),
                request.horizonMonths(),
                request.interruptionStartMonth(),
                request.interruptionMonths());
        return ResponseEntity.ok(IncomeInterruptionScenarioResponse.from(result));
    }
}
