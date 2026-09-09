package com.waypoint.scenarios.purchasereserve;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.waypoint.planning.runway.EmergencyFundRunwayCalculator;
import com.waypoint.planning.runway.RunwayStatus;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class PurchaseReserveImpactCalculatorTest {

    private final PurchaseReserveImpactCalculator calculator =
            new PurchaseReserveImpactCalculator(new EmergencyFundRunwayCalculator());

    @Test
    void computesTheDocumentedPrimaryScenario() {
        // Reserve 1000, purchase 400, expenses 300, income 100, floor 800:
        // remaining cash 600, funding gap 0, baseline floor gap 0, after
        // floor gap 200, finite runway changing from 5 to 3 months.
        PurchaseReserveImpactResult result = calculator.calculate(
                "PHP",
                new BigDecimal("1000.00"), new BigDecimal("400.00"),
                new BigDecimal("300.00"), new BigDecimal("100.00"),
                new BigDecimal("800.00"));

        assertThat(result.currency()).isEqualTo("PHP");
        assertThat(result.reserveAfterPurchase()).isEqualByComparingTo("600.00");
        assertThat(result.purchaseFundingGap()).isEqualByComparingTo("0.00");
        assertThat(result.purchaseFitsAvailableCash()).isTrue();
        assertThat(result.baselineReserveFloorGap()).isEqualByComparingTo("0.00");
        assertThat(result.reserveFloorGapAfterPurchase()).isEqualByComparingTo("200.00");
        assertThat(result.reserveMeetsFloorAfterPurchase()).isFalse();

        assertThat(result.beforePurchaseRunway().status()).isEqualTo(RunwayStatus.FINITE);
        assertThat(result.beforePurchaseRunway().runwayMonths()).isEqualByComparingTo("5.00");
        assertThat(result.beforePurchaseRunway().fullMonthsCovered()).isEqualTo(BigInteger.valueOf(5));

        assertThat(result.afterPurchaseRunwayAvailability())
                .isEqualTo(AfterPurchaseRunwayAvailability.AVAILABLE);
        assertThat(result.afterPurchaseRunway().status()).isEqualTo(RunwayStatus.FINITE);
        assertThat(result.afterPurchaseRunway().runwayMonths()).isEqualByComparingTo("3.00");
        assertThat(result.afterPurchaseRunway().fullMonthsCovered()).isEqualTo(BigInteger.valueOf(3));
    }

    @Test
    void aPurchaseAboveReserveReturnsANegativeCashBalanceAndExactFundingGapWithUnavailableAfterRunway() {
        PurchaseReserveImpactResult result = calculator.calculate(
                "USD",
                new BigDecimal("1000.00"), new BigDecimal("1200.00"),
                new BigDecimal("300.00"), new BigDecimal("100.00"),
                new BigDecimal("800.00"));

        assertThat(result.reserveAfterPurchase()).isEqualByComparingTo("-200.00");
        assertThat(result.purchaseFundingGap()).isEqualByComparingTo("200.00");
        assertThat(result.purchaseFitsAvailableCash()).isFalse();
        assertThat(result.reserveFloorGapAfterPurchase()).isEqualByComparingTo("1000.00");
        assertThat(result.afterPurchaseRunwayAvailability())
                .isEqualTo(AfterPurchaseRunwayAvailability.INSUFFICIENT_CASH);
        assertThat(result.afterPurchaseRunway()).isNull();
        // The before-purchase runway is still computed from the valid, unaffected reserve.
        assertThat(result.beforePurchaseRunway().status()).isEqualTo(RunwayStatus.FINITE);
    }

    @Test
    void aPurchaseExactlyEqualToTheReserveReturnsAValidZeroReserveCalculation() {
        PurchaseReserveImpactResult result = calculator.calculate(
                "USD",
                new BigDecimal("1000.00"), new BigDecimal("1000.00"),
                new BigDecimal("300.00"), new BigDecimal("100.00"),
                new BigDecimal("800.00"));

        assertThat(result.reserveAfterPurchase()).isEqualByComparingTo("0.00");
        assertThat(result.purchaseFundingGap()).isEqualByComparingTo("0.00");
        assertThat(result.purchaseFitsAvailableCash()).isTrue();
        assertThat(result.afterPurchaseRunwayAvailability())
                .isEqualTo(AfterPurchaseRunwayAvailability.AVAILABLE);
        assertThat(result.afterPurchaseRunway().status()).isEqualTo(RunwayStatus.FINITE);
        assertThat(result.afterPurchaseRunway().runwayMonths()).isEqualByComparingTo("0.00");
        assertThat(result.afterPurchaseRunway().fullMonthsCovered()).isEqualTo(BigInteger.ZERO);
    }

    @Test
    void zeroPurchasePreservesTheBaselineReserveAndBothRunways() {
        PurchaseReserveImpactResult result = calculator.calculate(
                "USD",
                new BigDecimal("500.00"), BigDecimal.ZERO,
                new BigDecimal("200.00"), new BigDecimal("50.00"),
                new BigDecimal("300.00"));

        assertThat(result.reserveAfterPurchase()).isEqualByComparingTo("500.00");
        assertThat(result.purchaseFundingGap()).isEqualByComparingTo("0.00");
        assertThat(result.baselineReserveFloorGap()).isEqualByComparingTo(result.reserveFloorGapAfterPurchase());
        assertThat(result.beforePurchaseRunway()).isEqualTo(result.afterPurchaseRunway());
    }

    @Test
    void incomeCoveringExpensesPreservesNoShortfallAndNullSemanticsOnBothRunways() {
        PurchaseReserveImpactResult result = calculator.calculate(
                "USD",
                new BigDecimal("1000.00"), new BigDecimal("200.00"),
                new BigDecimal("300.00"), new BigDecimal("400.00"),
                new BigDecimal("100.00"));

        assertThat(result.beforePurchaseRunway().status()).isEqualTo(RunwayStatus.NO_SHORTFALL);
        assertThat(result.beforePurchaseRunway().runwayMonths()).isNull();
        assertThat(result.beforePurchaseRunway().fullMonthsCovered()).isNull();

        assertThat(result.afterPurchaseRunwayAvailability())
                .isEqualTo(AfterPurchaseRunwayAvailability.AVAILABLE);
        assertThat(result.afterPurchaseRunway().status()).isEqualTo(RunwayStatus.NO_SHORTFALL);
        assertThat(result.afterPurchaseRunway().runwayMonths()).isNull();
        assertThat(result.afterPurchaseRunway().fullMonthsCovered()).isNull();
    }

    @Test
    void aZeroFloorNeverProducesAFloorGap() {
        PurchaseReserveImpactResult result = calculator.calculate(
                "USD",
                new BigDecimal("500.00"), new BigDecimal("100.00"),
                new BigDecimal("200.00"), new BigDecimal("50.00"),
                BigDecimal.ZERO);

        assertThat(result.baselineReserveFloorGap()).isEqualByComparingTo("0.00");
        assertThat(result.reserveFloorGapAfterPurchase()).isEqualByComparingTo("0.00");
        assertThat(result.reserveMeetsFloorAfterPurchase()).isTrue();
    }

    @Test
    void anAlreadyBreachedFloorIsDistinguishableFromThePurchaseImpact() {
        PurchaseReserveImpactResult result = calculator.calculate(
                "USD",
                new BigDecimal("500.00"), new BigDecimal("100.00"),
                new BigDecimal("200.00"), new BigDecimal("50.00"),
                new BigDecimal("900.00"));

        assertThat(result.baselineReserveFloorGap()).isEqualByComparingTo("400.00");
        assertThat(result.reserveFloorGapAfterPurchase()).isEqualByComparingTo("500.00");
        assertThat(result.reserveMeetsFloorAfterPurchase()).isFalse();
        // The purchase itself still fits available cash; only the floor is unmet.
        assertThat(result.purchaseFitsAvailableCash()).isTrue();
    }

    @Test
    void computesAnExactFundingGapForArbitraryDecimalInputs() {
        PurchaseReserveImpactResult result = calculator.calculate(
                "USD",
                new BigDecimal("250.75"), new BigDecimal("300.00"),
                new BigDecimal("100.00"), new BigDecimal("0"),
                BigDecimal.ZERO);

        assertThat(result.reserveAfterPurchase()).isEqualByComparingTo("-49.25");
        assertThat(result.purchaseFundingGap()).isEqualByComparingTo("49.25");
    }

    @Test
    void identicalInputsProduceIdenticalResults() {
        PurchaseReserveImpactResult first = calculator.calculate(
                "USD",
                new BigDecimal("1000.00"), new BigDecimal("400.00"),
                new BigDecimal("300.00"), new BigDecimal("100.00"),
                new BigDecimal("800.00"));
        PurchaseReserveImpactResult second = calculator.calculate(
                "USD",
                new BigDecimal("1000.00"), new BigDecimal("400.00"),
                new BigDecimal("300.00"), new BigDecimal("100.00"),
                new BigDecimal("800.00"));

        assertThat(first).isEqualTo(second);
    }

    @Test
    void normalizesCurrencyToUppercaseIndependentlyOfDefaultLocale() {
        Locale originalDefault = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
        try {
            PurchaseReserveImpactResult result = calculator.calculate(
                    "inr",
                    new BigDecimal("100.00"), BigDecimal.ZERO,
                    new BigDecimal("50.00"), BigDecimal.ZERO,
                    BigDecimal.ZERO);

            assertThat(result.currency()).isEqualTo("INR");
        } finally {
            Locale.setDefault(originalDefault);
        }
    }

    @Test
    void rejectsATwoCharacterCodeThatExpandsToThreeUppercaseLetters() {
        assertThatThrownBy(() -> calculator.calculate(
                "ßa",
                new BigDecimal("100.00"), BigDecimal.ZERO,
                new BigDecimal("50.00"), BigDecimal.ZERO,
                BigDecimal.ZERO))
                .isInstanceOf(InvalidPurchaseReserveImpactInputException.class)
                .hasMessageContaining("3-letter");
    }

    @Test
    void rejectsNullCurrency() {
        assertThatThrownBy(() -> calculator.calculate(
                null, new BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))
                .isInstanceOf(InvalidPurchaseReserveImpactInputException.class)
                .hasMessageContaining("currency");
    }

    @Test
    void rejectsBlankCurrency() {
        assertThatThrownBy(() -> calculator.calculate(
                "   ", new BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))
                .isInstanceOf(InvalidPurchaseReserveImpactInputException.class)
                .hasMessageContaining("currency");
    }

    @Test
    void rejectsMalformedCurrencyCode() {
        assertThatThrownBy(() -> calculator.calculate(
                "US1", new BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))
                .isInstanceOf(InvalidPurchaseReserveImpactInputException.class)
                .hasMessageContaining("3-letter");
    }

    @Test
    void rejectsNullAvailableReserve() {
        assertThatThrownBy(() -> calculator.calculate(
                "USD", null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))
                .isInstanceOf(InvalidPurchaseReserveImpactInputException.class)
                .hasMessageContaining("availableReserve");
    }

    @Test
    void rejectsNullPurchaseAmount() {
        assertThatThrownBy(() -> calculator.calculate(
                "USD", new BigDecimal("100.00"), null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))
                .isInstanceOf(InvalidPurchaseReserveImpactInputException.class)
                .hasMessageContaining("purchaseAmount");
    }

    @Test
    void rejectsNullMonthlyExpenses() {
        assertThatThrownBy(() -> calculator.calculate(
                "USD", new BigDecimal("100.00"), BigDecimal.ZERO, null, BigDecimal.ZERO, BigDecimal.ZERO))
                .isInstanceOf(InvalidPurchaseReserveImpactInputException.class)
                .hasMessageContaining("monthlyExpenses");
    }

    @Test
    void rejectsNullMonthlyNetIncome() {
        assertThatThrownBy(() -> calculator.calculate(
                "USD", new BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal.ZERO, null, BigDecimal.ZERO))
                .isInstanceOf(InvalidPurchaseReserveImpactInputException.class)
                .hasMessageContaining("monthlyNetIncome");
    }

    @Test
    void rejectsNullMinimumReserve() {
        assertThatThrownBy(() -> calculator.calculate(
                "USD", new BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null))
                .isInstanceOf(InvalidPurchaseReserveImpactInputException.class)
                .hasMessageContaining("minimumReserve");
    }

    @Test
    void rejectsNegativePurchaseAmount() {
        assertThatThrownBy(() -> calculator.calculate(
                "USD", new BigDecimal("100.00"), new BigDecimal("-0.01"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))
                .isInstanceOf(InvalidPurchaseReserveImpactInputException.class)
                .hasMessageContaining("must not be negative");
    }

    @Test
    void rejectsNegativeMinimumReserve() {
        assertThatThrownBy(() -> calculator.calculate(
                "USD", new BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("-1.00")))
                .isInstanceOf(InvalidPurchaseReserveImpactInputException.class)
                .hasMessageContaining("must not be negative");
    }

    @Test
    void rejectsExcessiveFractionDigits() {
        assertThatThrownBy(() -> calculator.calculate(
                "USD", new BigDecimal("100.001"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))
                .isInstanceOf(InvalidPurchaseReserveImpactInputException.class)
                .hasMessageContaining("fraction digits");
    }

    @Test
    void rejectsExcessiveIntegerDigits() {
        BigDecimal tooLarge = new BigDecimal("100000000000000000.00");
        assertThatThrownBy(() -> calculator.calculate(
                "USD", tooLarge, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))
                .isInstanceOf(InvalidPurchaseReserveImpactInputException.class)
                .hasMessageContaining("integer digits");
    }

    @Test
    void rejectsIntegerDigitLimitBypassedByNegativeScaleRepresentation() {
        BigDecimal negativeScaleTooLarge = new BigDecimal("1E+17");
        assertThatThrownBy(() -> calculator.calculate(
                "USD", negativeScaleTooLarge, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))
                .isInstanceOf(InvalidPurchaseReserveImpactInputException.class)
                .hasMessageContaining("integer digits");
    }
}
