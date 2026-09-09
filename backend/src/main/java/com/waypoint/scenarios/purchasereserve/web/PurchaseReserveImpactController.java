package com.waypoint.scenarios.purchasereserve.web;

import com.waypoint.planning.runway.EmergencyFundRunwayCalculator;
import com.waypoint.scenarios.purchasereserve.InvalidPurchaseReserveImpactInputException;
import com.waypoint.scenarios.purchasereserve.PurchaseReserveImpactCalculator;
import com.waypoint.scenarios.purchasereserve.PurchaseReserveImpactResult;
import com.waypoint.scenarios.purchasereserve.web.dto.PurchaseReserveImpactRequest;
import com.waypoint.scenarios.purchasereserve.web.dto.PurchaseReserveImpactResponse;
import com.waypoint.web.ErrorResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stateless purchase-impact-on-reserves scenario calculation. Accepts no
 * household or entity identifier and reads no persisted state; every input
 * is an explicit, disposable modeling value supplied on the request, and
 * before/after coverage reuses the merged {@link EmergencyFundRunwayCalculator}
 * through a direct Java call rather than an internal HTTP request.
 *
 * <p>The {@link InvalidPurchaseReserveImpactInputException} handler below is
 * scoped to this controller only (a Spring {@code @ExceptionHandler} method
 * declared on a controller takes precedence over a global
 * {@code @RestControllerAdvice} for exceptions raised within that
 * controller), so it cannot intercept a sibling controller's errors and
 * requires no change to the shared {@code ApiExceptionHandler}.
 */
@RestController
@RequestMapping("/api/scenarios/purchase-reserve-impact")
public class PurchaseReserveImpactController {

    private final PurchaseReserveImpactCalculator calculator;

    public PurchaseReserveImpactController(PurchaseReserveImpactCalculator calculator) {
        this.calculator = calculator;
    }

    @PostMapping
    public ResponseEntity<PurchaseReserveImpactResponse> calculate(
            @Valid @RequestBody PurchaseReserveImpactRequest request
    ) {
        PurchaseReserveImpactResult result = calculator.calculate(
                request.currency(),
                request.availableReserve(),
                request.purchaseAmount(),
                request.monthlyExpenses(),
                request.monthlyNetIncome(),
                request.minimumReserve());
        return ResponseEntity.ok(PurchaseReserveImpactResponse.from(result));
    }

    @ExceptionHandler(InvalidPurchaseReserveImpactInputException.class)
    public ResponseEntity<ErrorResponse> handleInvalidInput(InvalidPurchaseReserveImpactInputException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("VALIDATION_FAILED", ex.getMessage(), List.of()));
    }
}
