package com.waypoint.review.planningassumptions.web;

import com.waypoint.review.planningassumptions.InvalidPlanningAssumptionReviewInputException;
import com.waypoint.review.planningassumptions.PlanningAssumptionReviewResult;
import com.waypoint.review.planningassumptions.PlanningAssumptionReviewService;
import com.waypoint.review.planningassumptions.web.dto.PlanningAssumptionReviewResponse;
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
 * Read-only review of a household's current unsuperseded planning
 * assumptions: which are due or overdue for review, and which have expired
 * or not yet started their effective window, as of an explicit caller-
 * supplied {@code asOf} date. Mounted under its own {@code /planning-
 * assumptions/review} path — distinct from
 * {@code /assumptions/{assumptionId}} on {@code PlanningAssumptionController}
 * — so it is never mistaken for a lookup by assumption id. Performs no
 * write, supersession, or review-date reset.
 *
 * <p>The {@link InvalidPlanningAssumptionReviewInputException} handler below
 * is a controller-local {@code @ExceptionHandler}, kept separate from the
 * shared {@code com.waypoint.web.ApiExceptionHandler} for the same reason as
 * the sibling financial-data-freshness review. In practice {@code asOf} is
 * always present by the time this handler could fire, since Spring rejects
 * a missing or malformed {@code asOf} before the controller method runs
 * (via the shared advice's existing
 * {@code MissingServletRequestParameterException}/
 * {@code MethodArgumentTypeMismatchException} handling); it exists as a
 * defensive guard around the domain layer's own invariant. Unknown
 * households are handled by the shared advice's existing
 * {@code HouseholdNotFoundException} path.
 */
@RestController
@RequestMapping("/api/households/{householdId}/planning-assumptions/review")
public class PlanningAssumptionReviewController {

    private final PlanningAssumptionReviewService planningAssumptionReviewService;

    public PlanningAssumptionReviewController(PlanningAssumptionReviewService planningAssumptionReviewService) {
        this.planningAssumptionReviewService = planningAssumptionReviewService;
    }

    @GetMapping
    public ResponseEntity<PlanningAssumptionReviewResponse> review(
            @PathVariable UUID householdId,
            @RequestParam LocalDate asOf
    ) {
        PlanningAssumptionReviewResult result = planningAssumptionReviewService.review(householdId, asOf);
        return ResponseEntity.ok(PlanningAssumptionReviewResponse.from(result));
    }

    @ExceptionHandler(InvalidPlanningAssumptionReviewInputException.class)
    public ResponseEntity<ErrorResponse> handleInvalidInput(InvalidPlanningAssumptionReviewInputException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("VALIDATION_FAILED", ex.getMessage(), List.of()));
    }
}
