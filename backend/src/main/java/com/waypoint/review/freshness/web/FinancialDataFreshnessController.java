package com.waypoint.review.freshness.web;

import com.waypoint.review.freshness.FinancialDataFreshnessResult;
import com.waypoint.review.freshness.FinancialDataFreshnessService;
import com.waypoint.review.freshness.InvalidFreshnessReviewInputException;
import com.waypoint.review.freshness.web.dto.FinancialDataFreshnessResponse;
import com.waypoint.web.ErrorResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only financial-data-freshness review over a household's current
 * asset and liability source rows. Reads no financial amount into the
 * response and performs no write, migration, or scheduling.
 *
 * <p>The {@link InvalidFreshnessReviewInputException} handler below is a
 * controller-local {@code @ExceptionHandler}, which Spring resolves before
 * a shared {@code @RestControllerAdvice} for exceptions raised within this
 * controller, so it cannot intercept a sibling controller's errors and
 * requires no change to the shared {@code ApiExceptionHandler}. Unknown
 * households and malformed/missing request parameters are already handled
 * by that shared advice via the existing {@code HouseholdNotFoundException},
 * {@code MethodArgumentTypeMismatchException}, and
 * {@code MissingServletRequestParameterException} paths.
 */
@RestController
@RequestMapping("/api/households/{householdId}/financial-data-freshness")
public class FinancialDataFreshnessController {

    private final FinancialDataFreshnessService financialDataFreshnessService;

    public FinancialDataFreshnessController(FinancialDataFreshnessService financialDataFreshnessService) {
        this.financialDataFreshnessService = financialDataFreshnessService;
    }

    @GetMapping
    public ResponseEntity<FinancialDataFreshnessResponse> review(
            @PathVariable UUID householdId,
            @RequestParam LocalDate reviewDate,
            @RequestParam int maxAgeDays
    ) {
        FinancialDataFreshnessResult result =
                financialDataFreshnessService.review(householdId, reviewDate, maxAgeDays);
        return ResponseEntity.ok(FinancialDataFreshnessResponse.from(result));
    }

    @ExceptionHandler(InvalidFreshnessReviewInputException.class)
    public ResponseEntity<ErrorResponse> handleInvalidInput(InvalidFreshnessReviewInputException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("VALIDATION_FAILED", ex.getMessage(), List.of()));
    }
}
