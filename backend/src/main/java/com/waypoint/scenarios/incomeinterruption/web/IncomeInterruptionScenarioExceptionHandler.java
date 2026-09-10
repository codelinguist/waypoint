package com.waypoint.scenarios.incomeinterruption.web;

import com.waypoint.scenarios.incomeinterruption.InvalidIncomeInterruptionScenarioInputException;
import com.waypoint.web.ErrorResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Reuses the shared {@link ErrorResponse} shape read-only, without editing
 * the shared {@code ApiExceptionHandler}. Scoped to {@link IncomeInterruptionScenarioController}
 * only, so it never intercepts another controller's exceptions.
 */
@RestControllerAdvice(assignableTypes = IncomeInterruptionScenarioController.class)
public class IncomeInterruptionScenarioExceptionHandler {

    @ExceptionHandler(InvalidIncomeInterruptionScenarioInputException.class)
    public ResponseEntity<ErrorResponse> handleInvalidInput(InvalidIncomeInterruptionScenarioInputException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("VALIDATION_FAILED", ex.getMessage(), List.of()));
    }
}
