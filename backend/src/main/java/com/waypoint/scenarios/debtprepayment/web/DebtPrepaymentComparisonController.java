package com.waypoint.scenarios.debtprepayment.web;

import com.waypoint.scenarios.debtprepayment.DebtPrepaymentComparisonCalculator;
import com.waypoint.scenarios.debtprepayment.DebtPrepaymentComparisonResult;
import com.waypoint.scenarios.debtprepayment.web.dto.DebtPrepaymentComparisonRequest;
import com.waypoint.scenarios.debtprepayment.web.dto.DebtPrepaymentComparisonResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stateless comparison of an explicit immediate principal prepayment against continuing the same
 * fixed monthly debt payment. Every input is a temporary, caller-supplied modeling value; nothing
 * is read from or written to household state.
 */
@RestController
@RequestMapping("/api/scenarios/debt-prepayment")
public class DebtPrepaymentComparisonController {

    @PostMapping
    public ResponseEntity<DebtPrepaymentComparisonResponse> compare(
            @Valid @RequestBody DebtPrepaymentComparisonRequest request
    ) {
        DebtPrepaymentComparisonResult result = DebtPrepaymentComparisonCalculator.calculate(
                request.principal(),
                request.monthlyInterestRate(),
                request.monthlyPayment(),
                request.currency(),
                request.immediatePrepayment()
        );
        return ResponseEntity.ok(DebtPrepaymentComparisonResponse.from(result));
    }
}
