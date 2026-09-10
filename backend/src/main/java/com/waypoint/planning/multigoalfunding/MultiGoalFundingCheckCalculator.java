package com.waypoint.planning.multigoalfunding;

import com.waypoint.planning.goalcontribution.GoalContributionCalculator;
import com.waypoint.planning.goalcontribution.GoalContributionResult;
import com.waypoint.planning.goalcontribution.InvalidGoalContributionInputException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * Checks whether several explicitly modeled saving goals fit one shared
 * monthly budget, without choosing household priorities between them.
 *
 * <p>This is a disposable, stateless calculation over caller-supplied
 * temporary inputs: it assumes zero growth, fees and withdrawals, all goals
 * start contributing in the first modeled month, and it does not read or
 * write any persisted household state. Each goal's required monthly
 * contribution is calculated independently by reusing the accepted
 * {@link GoalContributionCalculator} read-only; this class never chooses a
 * priority order or reallocates budget between goals. It enforces its own
 * input invariants so it rejects invalid values whether it is called
 * directly or through the HTTP layer, which applies the same rules
 * independently via request validation.
 */
@Service
public class MultiGoalFundingCheckCalculator {

    private static final int MAX_INTEGER_DIGITS = 17;
    private static final int MAX_FRACTION_DIGITS = 2;
    private static final int MIN_GOALS = 1;
    private static final int MAX_GOALS = 50;
    private static final int MAX_REFERENCE_LENGTH = 64;
    private static final Pattern CURRENCY_CODE = Pattern.compile("^[A-Za-z]{3}$");
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(MAX_FRACTION_DIGITS);

    private final GoalContributionCalculator goalContributionCalculator;

    public MultiGoalFundingCheckCalculator(GoalContributionCalculator goalContributionCalculator) {
        this.goalContributionCalculator = goalContributionCalculator;
    }

    public MultiGoalFundingCheckResult calculate(
            String currency, BigDecimal availableMonthlyBudget, List<GoalFundingInput> goals
    ) {
        String normalizedCurrency = validateCurrency(currency);
        BigDecimal normalizedBudget = validateAmount(availableMonthlyBudget, "availableMonthlyBudget");
        validateGoalCount(goals);

        List<GoalFundingCheckItemResult> goalResults = new ArrayList<>(goals.size());
        Set<String> seenReferences = new HashSet<>();
        BigDecimal totalRequiredMonthlyContribution = ZERO;

        for (GoalFundingInput goal : goals) {
            if (goal == null) {
                throw new InvalidMultiGoalFundingInputException("goals must not contain null entries");
            }
            String reference = validateReference(goal.reference(), seenReferences);
            GoalContributionResult contribution = calculateGoalContribution(reference, goal, normalizedCurrency);
            goalResults.add(new GoalFundingCheckItemResult(reference, contribution));
            totalRequiredMonthlyContribution = totalRequiredMonthlyContribution.add(contribution.monthlyContribution());
        }

        BigDecimal budgetMinusRequired = normalizedBudget.subtract(totalRequiredMonthlyContribution);
        BigDecimal shortfall = ZERO.max(budgetMinusRequired.negate());
        BigDecimal unallocatedBudget = ZERO.max(budgetMinusRequired);
        MultiGoalFundingStatus status = budgetMinusRequired.signum() >= 0
                ? MultiGoalFundingStatus.FITS
                : MultiGoalFundingStatus.SHORTFALL;

        return new MultiGoalFundingCheckResult(
                normalizedCurrency,
                normalizedBudget,
                List.copyOf(goalResults),
                totalRequiredMonthlyContribution,
                budgetMinusRequired,
                shortfall,
                unallocatedBudget,
                status
        );
    }

    private GoalContributionResult calculateGoalContribution(
            String reference, GoalFundingInput goal, String normalizedCurrency
    ) {
        try {
            return goalContributionCalculator.calculate(
                    normalizedCurrency, goal.targetAmount(), goal.currentAmount(), goal.contributionMonths());
        } catch (InvalidGoalContributionInputException ex) {
            throw new InvalidMultiGoalFundingInputException(
                    "goal '" + reference + "': " + ex.getMessage());
        }
    }

    private String validateCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            throw new InvalidMultiGoalFundingInputException("currency must not be blank");
        }
        if (!CURRENCY_CODE.matcher(currency).matches()) {
            throw new InvalidMultiGoalFundingInputException("currency must be a 3-letter currency code");
        }
        return currency.toUpperCase(Locale.ROOT);
    }

    private BigDecimal validateAmount(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new InvalidMultiGoalFundingInputException(fieldName + " must not be null");
        }
        if (value.scale() > MAX_FRACTION_DIGITS) {
            throw new InvalidMultiGoalFundingInputException(
                    fieldName + " must have at most " + MAX_FRACTION_DIGITS + " fraction digits");
        }
        int integerDigits = Math.max(value.precision() - value.scale(), 0);
        if (integerDigits > MAX_INTEGER_DIGITS) {
            throw new InvalidMultiGoalFundingInputException(
                    fieldName + " must have at most " + MAX_INTEGER_DIGITS + " integer digits");
        }
        if (value.signum() < 0) {
            throw new InvalidMultiGoalFundingInputException(fieldName + " must be at least zero");
        }
        return value.setScale(MAX_FRACTION_DIGITS, RoundingMode.UNNECESSARY);
    }

    private void validateGoalCount(List<GoalFundingInput> goals) {
        if (goals == null || goals.size() < MIN_GOALS) {
            throw new InvalidMultiGoalFundingInputException(
                    "goals must contain at least " + MIN_GOALS + " entry");
        }
        if (goals.size() > MAX_GOALS) {
            throw new InvalidMultiGoalFundingInputException(
                    "goals must contain at most " + MAX_GOALS + " entries");
        }
    }

    private String validateReference(String reference, Set<String> seenReferences) {
        if (reference == null || reference.isBlank()) {
            throw new InvalidMultiGoalFundingInputException("goal reference must not be blank");
        }
        if (reference.length() > MAX_REFERENCE_LENGTH) {
            throw new InvalidMultiGoalFundingInputException(
                    "goal reference must be at most " + MAX_REFERENCE_LENGTH + " characters");
        }
        if (!seenReferences.add(reference)) {
            throw new InvalidMultiGoalFundingInputException("duplicate goal reference: " + reference);
        }
        return reference;
    }
}
