package com.waypoint.scenarios.purchasereserve;

import com.waypoint.planning.runway.EmergencyFundRunwayCalculator;
import com.waypoint.planning.runway.EmergencyFundRunwayResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * Pure, stateless calculation of how one explicitly supplied cash purchase
 * would change reserve coverage against an explicitly supplied reserve
 * floor, from caller-supplied temporary inputs only. Callable directly,
 * independently of HTTP and persistence; it enforces its own input
 * invariants rather than trusting transport-layer validation alone.
 *
 * <p>Before/after coverage reuses the merged {@link EmergencyFundRunwayCalculator}
 * through a direct Java call, read-only: its arithmetic, rounding, and
 * {@code FINITE}/{@code NO_SHORTFALL} conventions are neither duplicated nor
 * altered here. When the purchase exceeds the available reserve, the
 * after-purchase reserve is negative and is never passed to that
 * calculator, which rejects a negative reserve rather than silently
 * clamping it to zero; the after-purchase runway is instead reported as
 * explicitly unavailable.
 */
@Service
public class PurchaseReserveImpactCalculator {

    private static final Pattern CURRENCY_PATTERN = Pattern.compile("^[A-Za-z]{3}$");
    private static final int MAX_INTEGER_DIGITS = 17;
    private static final int SCALE = 2;

    private final EmergencyFundRunwayCalculator runwayCalculator;

    public PurchaseReserveImpactCalculator(EmergencyFundRunwayCalculator runwayCalculator) {
        this.runwayCalculator = runwayCalculator;
    }

    public PurchaseReserveImpactResult calculate(
            String currency,
            BigDecimal availableReserve,
            BigDecimal purchaseAmount,
            BigDecimal monthlyExpenses,
            BigDecimal monthlyNetIncome,
            BigDecimal minimumReserve
    ) {
        String normalizedCurrency = normalizeCurrency(currency);
        validateAmount(availableReserve, "availableReserve");
        validateAmount(purchaseAmount, "purchaseAmount");
        validateAmount(monthlyExpenses, "monthlyExpenses");
        validateAmount(monthlyNetIncome, "monthlyNetIncome");
        validateAmount(minimumReserve, "minimumReserve");

        BigDecimal reserveAfterPurchase = availableReserve.subtract(purchaseAmount).setScale(SCALE, RoundingMode.UNNECESSARY);
        BigDecimal purchaseFundingGap = positiveGap(BigDecimal.ZERO, reserveAfterPurchase);
        boolean purchaseFitsAvailableCash = purchaseFundingGap.signum() == 0;

        BigDecimal baselineReserveFloorGap = positiveGap(minimumReserve, availableReserve);
        BigDecimal reserveFloorGapAfterPurchase = positiveGap(minimumReserve, reserveAfterPurchase);
        boolean reserveMeetsFloorAfterPurchase = reserveFloorGapAfterPurchase.signum() == 0;

        EmergencyFundRunwayResult beforePurchaseRunway = runwayCalculator.calculate(
                availableReserve, monthlyExpenses, monthlyNetIncome, normalizedCurrency);

        if (!purchaseFitsAvailableCash) {
            return new PurchaseReserveImpactResult(
                    normalizedCurrency,
                    availableReserve,
                    purchaseAmount,
                    monthlyExpenses,
                    monthlyNetIncome,
                    minimumReserve,
                    reserveAfterPurchase,
                    purchaseFundingGap,
                    false,
                    baselineReserveFloorGap,
                    reserveFloorGapAfterPurchase,
                    reserveMeetsFloorAfterPurchase,
                    beforePurchaseRunway,
                    AfterPurchaseRunwayAvailability.INSUFFICIENT_CASH,
                    null);
        }

        EmergencyFundRunwayResult afterPurchaseRunway = runwayCalculator.calculate(
                reserveAfterPurchase, monthlyExpenses, monthlyNetIncome, normalizedCurrency);

        return new PurchaseReserveImpactResult(
                normalizedCurrency,
                availableReserve,
                purchaseAmount,
                monthlyExpenses,
                monthlyNetIncome,
                minimumReserve,
                reserveAfterPurchase,
                purchaseFundingGap,
                true,
                baselineReserveFloorGap,
                reserveFloorGapAfterPurchase,
                reserveMeetsFloorAfterPurchase,
                beforePurchaseRunway,
                AfterPurchaseRunwayAvailability.AVAILABLE,
                afterPurchaseRunway);
    }

    private BigDecimal positiveGap(BigDecimal floor, BigDecimal reserve) {
        BigDecimal gap = floor.subtract(reserve);
        return gap.signum() > 0 ? gap.setScale(SCALE, RoundingMode.UNNECESSARY) : BigDecimal.ZERO.setScale(SCALE);
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            throw new InvalidPurchaseReserveImpactInputException("currency must not be blank");
        }
        String trimmed = currency.trim();
        if (!CURRENCY_PATTERN.matcher(trimmed).matches()) {
            throw new InvalidPurchaseReserveImpactInputException("currency must be a 3-letter currency code");
        }
        return trimmed.toUpperCase(Locale.ROOT);
    }

    private void validateAmount(BigDecimal amount, String fieldName) {
        if (amount == null) {
            throw new InvalidPurchaseReserveImpactInputException(fieldName + " must not be null");
        }
        if (amount.signum() < 0) {
            throw new InvalidPurchaseReserveImpactInputException(fieldName + " must not be negative");
        }
        if (amount.scale() > SCALE) {
            throw new InvalidPurchaseReserveImpactInputException(
                    fieldName + " must have at most " + SCALE + " fraction digits");
        }
        int integerDigits = Math.max(amount.precision() - amount.scale(), 0);
        if (integerDigits > MAX_INTEGER_DIGITS) {
            throw new InvalidPurchaseReserveImpactInputException(
                    fieldName + " must have at most " + MAX_INTEGER_DIGITS + " integer digits");
        }
    }
}
