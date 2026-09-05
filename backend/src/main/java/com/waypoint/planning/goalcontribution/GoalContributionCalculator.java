package com.waypoint.planning.goalcontribution;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * Finds the equal monthly saving amount needed to close an explicitly
 * supplied monetary goal gap over an explicit number of contributions.
 *
 * <p>This is a disposable, stateless calculation over caller-supplied
 * temporary inputs: it assumes zero growth, fees and withdrawals, does not
 * read or write any persisted household state, and never establishes
 * affordability or approves an allocation. It enforces its own input
 * invariants so it rejects invalid values whether it is called directly or
 * through the HTTP layer, which applies the same rules independently via
 * request validation.
 */
@Service
public class GoalContributionCalculator {

    private static final int MAX_INTEGER_DIGITS = 17;
    private static final int MAX_FRACTION_DIGITS = 2;
    private static final int MIN_CONTRIBUTION_MONTHS = 1;
    private static final int MAX_CONTRIBUTION_MONTHS = 1200;
    private static final Pattern CURRENCY_CODE = Pattern.compile("^[A-Za-z]{3}$");

    public GoalContributionResult calculate(
            String currency, BigDecimal targetAmount, BigDecimal currentAmount, int contributionMonths
    ) {
        String normalizedCurrency = validateCurrency(currency);
        BigDecimal normalizedTarget = validateAmount(targetAmount, "targetAmount", false);
        BigDecimal normalizedCurrent = validateAmount(currentAmount, "currentAmount", true);
        validateContributionMonths(contributionMonths);

        if (normalizedCurrent.compareTo(normalizedTarget) >= 0) {
            BigDecimal amountAboveTarget = normalizedCurrent.subtract(normalizedTarget).max(BigDecimal.ZERO);
            return new GoalContributionResult(
                    normalizedCurrency,
                    normalizedTarget,
                    normalizedCurrent,
                    contributionMonths,
                    BigDecimal.ZERO.setScale(MAX_FRACTION_DIGITS),
                    BigDecimal.ZERO.setScale(MAX_FRACTION_DIGITS),
                    BigDecimal.ZERO.setScale(MAX_FRACTION_DIGITS),
                    normalizedCurrent,
                    amountAboveTarget,
                    GoalContributionStatus.ALREADY_FUNDED
            );
        }

        BigDecimal remainingAmount = normalizedTarget.subtract(normalizedCurrent);
        BigDecimal monthlyContribution = remainingAmount.divide(
                BigDecimal.valueOf(contributionMonths), MAX_FRACTION_DIGITS, RoundingMode.CEILING);
        BigDecimal totalContributions = monthlyContribution.multiply(BigDecimal.valueOf(contributionMonths));
        BigDecimal projectedAmount = normalizedCurrent.add(totalContributions);
        BigDecimal amountAboveTarget = projectedAmount.subtract(normalizedTarget).max(BigDecimal.ZERO);

        return new GoalContributionResult(
                normalizedCurrency,
                normalizedTarget,
                normalizedCurrent,
                contributionMonths,
                remainingAmount,
                monthlyContribution,
                totalContributions,
                projectedAmount,
                amountAboveTarget,
                GoalContributionStatus.CONTRIBUTIONS_REQUIRED
        );
    }

    private String validateCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            throw new InvalidGoalContributionInputException("currency must not be blank");
        }
        if (!CURRENCY_CODE.matcher(currency).matches()) {
            throw new InvalidGoalContributionInputException("currency must be a 3-letter currency code");
        }
        return currency.toUpperCase();
    }

    private BigDecimal validateAmount(BigDecimal value, String fieldName, boolean allowZero) {
        if (value == null) {
            throw new InvalidGoalContributionInputException(fieldName + " must not be null");
        }
        if (value.scale() > MAX_FRACTION_DIGITS) {
            throw new InvalidGoalContributionInputException(
                    fieldName + " must have at most " + MAX_FRACTION_DIGITS + " fraction digits");
        }
        int integerDigits = value.precision() - Math.max(value.scale(), 0);
        if (integerDigits > MAX_INTEGER_DIGITS) {
            throw new InvalidGoalContributionInputException(
                    fieldName + " must have at most " + MAX_INTEGER_DIGITS + " integer digits");
        }
        if (allowZero ? value.signum() < 0 : value.signum() <= 0) {
            throw new InvalidGoalContributionInputException(
                    fieldName + " must be " + (allowZero ? "at least zero" : "greater than zero"));
        }
        return value.setScale(MAX_FRACTION_DIGITS, RoundingMode.UNNECESSARY);
    }

    private void validateContributionMonths(int contributionMonths) {
        if (contributionMonths < MIN_CONTRIBUTION_MONTHS || contributionMonths > MAX_CONTRIBUTION_MONTHS) {
            throw new InvalidGoalContributionInputException(
                    "contributionMonths must be between " + MIN_CONTRIBUTION_MONTHS
                            + " and " + MAX_CONTRIBUTION_MONTHS);
        }
    }
}
