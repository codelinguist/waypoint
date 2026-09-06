package com.waypoint.assumption.web;

import com.waypoint.assumption.InvalidPlanningAssumptionException;
import com.waypoint.assumption.PlanningAssumptionNotFoundException;
import com.waypoint.web.ErrorResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Reuses the shared {@link ErrorResponse} shape read-only, without editing
 * the shared {@code ApiExceptionHandler}. Scoped to
 * {@link PlanningAssumptionController} only, so it never intercepts another
 * controller's exceptions.
 */
@RestControllerAdvice(assignableTypes = PlanningAssumptionController.class)
public class PlanningAssumptionExceptionHandler {

    @ExceptionHandler(PlanningAssumptionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(PlanningAssumptionNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("PLANNING_ASSUMPTION_NOT_FOUND", ex.getMessage(), List.of()));
    }

    @ExceptionHandler(InvalidPlanningAssumptionException.class)
    public ResponseEntity<ErrorResponse> handleInvalid(InvalidPlanningAssumptionException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("VALIDATION_FAILED", ex.getMessage(), List.of()));
    }
}
