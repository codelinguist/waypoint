package com.waypoint.planning.runway.web;

import com.waypoint.planning.runway.EmergencyFundRunwayCalculator;
import com.waypoint.planning.runway.EmergencyFundRunwayResult;
import com.waypoint.planning.runway.InvalidRunwayInputException;
import com.waypoint.planning.runway.web.dto.EmergencyFundRunwayRequest;
import com.waypoint.planning.runway.web.dto.EmergencyFundRunwayResponse;
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
 * Stateless emergency-fund runway calculation. Accepts no household or
 * entity identifier and reads no persisted state; every input is an
 * explicit, disposable modeling value supplied on the request.
 *
 * <p>The {@link InvalidRunwayInputException} handler below is scoped to
 * this controller only (a Spring {@code @ExceptionHandler} method declared
 * on a controller takes precedence over a global {@code @RestControllerAdvice}
 * for exceptions raised within that controller), so it cannot intercept a
 * sibling controller's errors and requires no change to the shared
 * {@code ApiExceptionHandler}.
 */
@RestController
@RequestMapping("/api/planning/emergency-fund-runway")
public class EmergencyFundRunwayController {

    private final EmergencyFundRunwayCalculator calculator;

    public EmergencyFundRunwayController(EmergencyFundRunwayCalculator calculator) {
        this.calculator = calculator;
    }

    @PostMapping
    public ResponseEntity<EmergencyFundRunwayResponse> calculate(
            @Valid @RequestBody EmergencyFundRunwayRequest request
    ) {
        EmergencyFundRunwayResult result = calculator.calculate(
                request.availableReserve(),
                request.monthlyExpenses(),
                request.monthlyNetIncome(),
                request.currency());
        return ResponseEntity.ok(EmergencyFundRunwayResponse.from(result));
    }

    @ExceptionHandler(InvalidRunwayInputException.class)
    public ResponseEntity<ErrorResponse> handleInvalidInput(InvalidRunwayInputException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("VALIDATION_FAILED", ex.getMessage(), List.of()));
    }
}
