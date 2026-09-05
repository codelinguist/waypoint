package com.waypoint.planning.cashflow.web;

import com.waypoint.planning.cashflow.InvalidCashFlowProjectionInputException;
import com.waypoint.web.ErrorResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Reuses the shared {@link ErrorResponse} shape read-only, without editing
 * the shared {@code ApiExceptionHandler}. Scoped to {@link CashFlowProjectionController}
 * only, so it never intercepts another controller's exceptions.
 */
@RestControllerAdvice(assignableTypes = CashFlowProjectionController.class)
public class CashFlowProjectionExceptionHandler {

    @ExceptionHandler(InvalidCashFlowProjectionInputException.class)
    public ResponseEntity<ErrorResponse> handleInvalidInput(InvalidCashFlowProjectionInputException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("VALIDATION_FAILED", ex.getMessage(), List.of()));
    }
}
