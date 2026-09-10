package com.waypoint.assumption.web;

import com.waypoint.assumption.AssumptionAlreadySupersededException;
import com.waypoint.assumption.InvalidPlanningAssumptionException;
import com.waypoint.assumption.PlanningAssumptionNotFoundException;
import com.waypoint.web.ErrorResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * A module-local advice for the assumption package's own exception types.
 * Kept separate from {@code com.waypoint.web.ApiExceptionHandler} because
 * this task does not own that shared file; Spring dispatches to whichever
 * {@code @RestControllerAdvice} declares a handler for the thrown type, so
 * both advices coexist without conflict.
 */
@RestControllerAdvice
public class AssumptionExceptionHandler {

    @ExceptionHandler(PlanningAssumptionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAssumptionNotFound(PlanningAssumptionNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("PLANNING_ASSUMPTION_NOT_FOUND", ex.getMessage(), List.of()));
    }

    @ExceptionHandler(AssumptionAlreadySupersededException.class)
    public ResponseEntity<ErrorResponse> handleAlreadySuperseded(AssumptionAlreadySupersededException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("ASSUMPTION_ALREADY_SUPERSEDED", ex.getMessage(), List.of()));
    }

    @ExceptionHandler(InvalidPlanningAssumptionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidAssumption(InvalidPlanningAssumptionException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("VALIDATION_FAILED", ex.getMessage(), List.of()));
    }
}
