package com.waypoint.planning.debtamortization.web;

import com.waypoint.planning.debtamortization.DebtAmortizationCalculator;
import com.waypoint.planning.debtamortization.DebtAmortizationResult;
import com.waypoint.planning.debtamortization.web.dto.DebtAmortizationRequest;
import com.waypoint.planning.debtamortization.web.dto.DebtAmortizationResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stateless fixed-payment debt amortization calculation. Every input is a temporary,
 * caller-supplied modeling value; nothing is read from or written to household state.
 */
@RestController
@RequestMapping("/api/planning/debt-amortization")
public class DebtAmortizationController {

    @PostMapping
    public ResponseEntity<DebtAmortizationResponse> calculate(@Valid @RequestBody DebtAmortizationRequest request) {
        DebtAmortizationResult result = DebtAmortizationCalculator.calculate(
                request.principal(),
                request.monthlyInterestRate(),
                request.monthlyPayment(),
                request.currency()
        );
        return ResponseEntity.ok(DebtAmortizationResponse.from(result));
    }
}
