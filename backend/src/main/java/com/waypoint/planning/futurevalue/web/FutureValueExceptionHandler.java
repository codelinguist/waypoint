package com.waypoint.planning.futurevalue.web;

import com.waypoint.planning.futurevalue.InvalidFutureValueInputException;
import com.waypoint.web.ErrorResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Reuses the shared {@link ErrorResponse} shape read-only, without editing
 * the shared {@code ApiExceptionHandler}. Scoped to {@link FutureValueController}
 * only, so it never intercepts another controller's exceptions.
 */
@RestControllerAdvice(assignableTypes = FutureValueController.class)
public class FutureValueExceptionHandler {

    @ExceptionHandler(InvalidFutureValueInputException.class)
    public ResponseEntity<ErrorResponse> handleInvalidInput(InvalidFutureValueInputException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("VALIDATION_FAILED", ex.getMessage(), List.of()));
    }
}
