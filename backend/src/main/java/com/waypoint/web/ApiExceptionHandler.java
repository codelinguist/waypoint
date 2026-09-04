package com.waypoint.web;

import com.waypoint.household.AssetNotFoundException;
import com.waypoint.household.FinancialSnapshotNotFoundException;
import com.waypoint.household.HouseholdNotFoundException;
import com.waypoint.household.IncomeStreamNotFoundException;
import com.waypoint.household.InvalidAssetValueException;
import com.waypoint.household.InvalidScheduleException;
import com.waypoint.household.LiabilityNotFoundException;
import com.waypoint.household.ObligationNotFoundException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("VALIDATION_FAILED", "Request validation failed", details));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("MALFORMED_REQUEST", "Request body is malformed", List.of()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("MALFORMED_REQUEST", "Request parameter is malformed", List.of()));
    }

    @ExceptionHandler(HouseholdNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleHouseholdNotFound(HouseholdNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("HOUSEHOLD_NOT_FOUND", ex.getMessage(), List.of()));
    }

    @ExceptionHandler(AssetNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAssetNotFound(AssetNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("ASSET_NOT_FOUND", ex.getMessage(), List.of()));
    }

    @ExceptionHandler(LiabilityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleLiabilityNotFound(LiabilityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("LIABILITY_NOT_FOUND", ex.getMessage(), List.of()));
    }

    @ExceptionHandler(InvalidAssetValueException.class)
    public ResponseEntity<ErrorResponse> handleInvalidAssetValue(InvalidAssetValueException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("VALIDATION_FAILED", ex.getMessage(), List.of()));
    }

    @ExceptionHandler(IncomeStreamNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleIncomeStreamNotFound(IncomeStreamNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("INCOME_STREAM_NOT_FOUND", ex.getMessage(), List.of()));
    }

    @ExceptionHandler(ObligationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleObligationNotFound(ObligationNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("OBLIGATION_NOT_FOUND", ex.getMessage(), List.of()));
    }

    @ExceptionHandler(InvalidScheduleException.class)
    public ResponseEntity<ErrorResponse> handleInvalidSchedule(InvalidScheduleException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("VALIDATION_FAILED", ex.getMessage(), List.of()));
    }

    @ExceptionHandler(FinancialSnapshotNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleFinancialSnapshotNotFound(FinancialSnapshotNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("FINANCIAL_SNAPSHOT_NOT_FOUND", ex.getMessage(), List.of()));
    }
}
