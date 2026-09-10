package com.waypoint.scenarios.debtprepayment.web;

import com.waypoint.scenarios.debtprepayment.InvalidDebtPrepaymentInputException;
import com.waypoint.web.ErrorResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Reuses the shared {@link ErrorResponse} shape read-only, without editing the shared
 * {@code ApiExceptionHandler}. Scoped to {@link DebtPrepaymentComparisonController} only, so it
 * never intercepts another controller's exceptions.
 */
@RestControllerAdvice(assignableTypes = DebtPrepaymentComparisonController.class)
public class DebtPrepaymentComparisonExceptionHandler {

    @ExceptionHandler(InvalidDebtPrepaymentInputException.class)
    public ResponseEntity<ErrorResponse> handleInvalidInput(InvalidDebtPrepaymentInputException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("VALIDATION_FAILED", ex.getMessage(), List.of()));
    }
}
