package com.waypoint.scenarios.debtprepayment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.waypoint.planning.debtamortization.DebtAmortizationCalculator;
import com.waypoint.planning.debtamortization.DebtAmortizationResult;
import com.waypoint.planning.debtamortization.DebtAmortizationStatus;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class DebtPrepaymentComparisonCalculatorTest {

    @Test
    void matchesWorkedExampleWithZeroInterest() {
        DebtPrepaymentComparisonResult result = DebtPrepaymentComparisonCalculator.calculate(
                new BigDecimal("1000.00"), new BigDecimal("0"), new BigDecimal("300.00"), "USD",
                new BigDecimal("400.00"));

        assertThat(result.baseline().status()).isEqualTo(DebtAmortizationStatus.PAID_OFF);
        assertThat(result.baseline().payoffMonths()).isEqualTo(4);
        assertThat(result.scenario().status()).isEqualTo(DebtAmortizationStatus.PAID_OFF);
        assertThat(result.scenario().payoffMonths()).isEqualTo(2);
        assertThat(result.scenarioTotalCashPaid()).isEqualByComparingTo("1000.00");
        assertThat(result.lifetimeInterestSaved()).isEqualByComparingTo("0.00");
        assertThat(result.payoffMonthsSaved()).isEqualTo(2);
        assertThat(result.lifetimeCashSaved()).isEqualByComparingTo("0.00");
        assertThat(result.comparisonUnavailableReason()).isNull();
    }

    @Test
    void zeroPrepaymentProducesIdenticalPathsAndZeroSavings() {
        DebtPrepaymentComparisonResult result = DebtPrepaymentComparisonCalculator.calculate(
                new BigDecimal("100.00"), new BigDecimal("0.01"), new BigDecimal("60.00"), "USD",
                new BigDecimal("0.00"));

        assertThat(result.baseline().payoffMonths()).isEqualTo(result.scenario().payoffMonths());
        assertThat(result.baseline().totalPaid()).isEqualByComparingTo(result.scenario().totalPaid());
        assertThat(result.baseline().totalInterest()).isEqualByComparingTo(result.scenario().totalInterest());
        assertThat(result.scenarioTotalCashPaid()).isEqualByComparingTo(result.baseline().totalPaid());
        assertThat(result.lifetimeInterestSaved()).isEqualByComparingTo("0.00");
        assertThat(result.payoffMonthsSaved()).isEqualTo(0);
        assertThat(result.lifetimeCashSaved()).isEqualByComparingTo("0.00");
    }

    @Test
    void prepaymentEqualToPrincipalProducesZeroScenarioMonthsRetainingUpfrontCash() {
        DebtPrepaymentComparisonResult result = DebtPrepaymentComparisonCalculator.calculate(
                new BigDecimal("100.00"), new BigDecimal("0.01"), new BigDecimal("60.00"), "USD",
                new BigDecimal("100.00"));

        assertThat(result.baseline().status()).isEqualTo(DebtAmortizationStatus.PAID_OFF);
        assertThat(result.baseline().payoffMonths()).isEqualTo(2);
        assertThat(result.baseline().totalInterest()).isEqualByComparingTo("1.41");
        assertThat(result.baseline().totalPaid()).isEqualByComparingTo("101.41");

        assertThat(result.scenario().status()).isEqualTo(DebtAmortizationStatus.PAID_OFF);
        assertThat(result.scenario().payoffMonths()).isEqualTo(0);
        assertThat(result.scenario().schedule()).isEmpty();
        assertThat(result.scenario().totalPaid()).isEqualByComparingTo("0.00");

        assertThat(result.scenarioTotalCashPaid()).isEqualByComparingTo("100.00");
        assertThat(result.lifetimeInterestSaved()).isEqualByComparingTo("1.41");
        assertThat(result.payoffMonthsSaved()).isEqualTo(2);
        assertThat(result.lifetimeCashSaved()).isEqualByComparingTo("1.41");
    }

    @Test
    void interestBearingFixtureReconcilesScheduleTotalsAndUpfrontPayment() {
        BigDecimal principal = new BigDecimal("5000.00");
        BigDecimal rate = new BigDecimal("0.015");
        BigDecimal payment = new BigDecimal("150.00");
        BigDecimal prepayment = new BigDecimal("1000.00");

        DebtAmortizationResult expectedBaseline =
                DebtAmortizationCalculator.calculate(principal, rate, payment, "USD");
        DebtAmortizationResult expectedScenario =
                DebtAmortizationCalculator.calculate(principal.subtract(prepayment), rate, payment, "USD");

        DebtPrepaymentComparisonResult result =
                DebtPrepaymentComparisonCalculator.calculate(principal, rate, payment, "USD", prepayment);

        assertThat(result.baseline()).isEqualTo(expectedBaseline);
        assertThat(result.scenario()).isEqualTo(expectedScenario);
        assertThat(result.scenarioTotalCashPaid())
                .isEqualByComparingTo(prepayment.add(expectedScenario.totalPaid()));
        assertThat(result.lifetimeInterestSaved())
                .isEqualByComparingTo(expectedBaseline.totalInterest().subtract(expectedScenario.totalInterest()));
        assertThat(result.payoffMonthsSaved())
                .isEqualTo(expectedBaseline.payoffMonths() - expectedScenario.payoffMonths());
        assertThat(result.lifetimeCashSaved())
                .isEqualByComparingTo(expectedBaseline.totalPaid().subtract(result.scenarioTotalCashPaid()));
    }

    @Test
    void rejectsNegativePrepayment() {
        assertThatThrownBy(() -> DebtPrepaymentComparisonCalculator.calculate(
                new BigDecimal("1000.00"), new BigDecimal("0.01"), new BigDecimal("10.00"), "USD",
                new BigDecimal("-1.00")))
                .isInstanceOf(InvalidDebtPrepaymentInputException.class);
    }

    @Test
    void rejectsPrepaymentAbovePrincipal() {
        assertThatThrownBy(() -> DebtPrepaymentComparisonCalculator.calculate(
                new BigDecimal("1000.00"), new BigDecimal("0.01"), new BigDecimal("10.00"), "USD",
                new BigDecimal("1000.01")))
                .isInstanceOf(InvalidDebtPrepaymentInputException.class);
    }

    @Test
    void rejectsNullPrepayment() {
        assertThatThrownBy(() -> DebtPrepaymentComparisonCalculator.calculate(
                new BigDecimal("1000.00"), new BigDecimal("0.01"), new BigDecimal("10.00"), "USD", null))
                .isInstanceOf(InvalidDebtPrepaymentInputException.class);
    }

    @Test
    void rejectsPrepaymentWithExcessiveFractionalScale() {
        assertThatThrownBy(() -> DebtPrepaymentComparisonCalculator.calculate(
                new BigDecimal("1000.00"), new BigDecimal("0.01"), new BigDecimal("10.00"), "USD",
                new BigDecimal("100.005")))
                .isInstanceOf(InvalidDebtPrepaymentInputException.class);
    }

    @Test
    void rejectsPrepaymentWithExcessiveIntegerDigits() {
        assertThatThrownBy(() -> DebtPrepaymentComparisonCalculator.calculate(
                new BigDecimal("123456789012345678.00"), new BigDecimal("0.01"), new BigDecimal("10.00"), "USD",
                new BigDecimal("123456789012345678.00")))
                .isInstanceOf(InvalidDebtPrepaymentInputException.class);
    }

    @Test
    void propagatesInvalidPrincipalAsDebtPrepaymentException() {
        assertThatThrownBy(() -> DebtPrepaymentComparisonCalculator.calculate(
                new BigDecimal("-1.00"), new BigDecimal("0.01"), new BigDecimal("10.00"), "USD",
                new BigDecimal("0.00")))
                .isInstanceOf(InvalidDebtPrepaymentInputException.class);
    }

    @Test
    void propagatesInvalidCurrencyAsDebtPrepaymentException() {
        assertThatThrownBy(() -> DebtPrepaymentComparisonCalculator.calculate(
                new BigDecimal("1000.00"), new BigDecimal("0.01"), new BigDecimal("10.00"), "US1",
                new BigDecimal("0.00")))
                .isInstanceOf(InvalidDebtPrepaymentInputException.class);
    }

    @Test
    void bothNonAmortizingSuppressesLifetimeComparison() {
        DebtPrepaymentComparisonResult result = DebtPrepaymentComparisonCalculator.calculate(
                new BigDecimal("1000.00"), new BigDecimal("0.01"), new BigDecimal("10.00"), "USD",
                new BigDecimal("0.00"));

        assertThat(result.baseline().status()).isEqualTo(DebtAmortizationStatus.NON_AMORTIZING);
        assertThat(result.scenario().status()).isEqualTo(DebtAmortizationStatus.NON_AMORTIZING);
        assertThat(result.lifetimeInterestSaved()).isNull();
        assertThat(result.payoffMonthsSaved()).isNull();
        assertThat(result.lifetimeCashSaved()).isNull();
        assertThat(result.comparisonUnavailableReason()).isNotBlank();
    }

    @Test
    void prepaymentThatMakesOnlyScenarioAmortizingSuppressesLifetimeComparison() {
        DebtPrepaymentComparisonResult result = DebtPrepaymentComparisonCalculator.calculate(
                new BigDecimal("1000.00"), new BigDecimal("0.01"), new BigDecimal("10.00"), "USD",
                new BigDecimal("700.00"));

        assertThat(result.baseline().status()).isEqualTo(DebtAmortizationStatus.NON_AMORTIZING);
        assertThat(result.scenario().status()).isNotEqualTo(DebtAmortizationStatus.NON_AMORTIZING);
        assertThat(result.lifetimeInterestSaved()).isNull();
        assertThat(result.payoffMonthsSaved()).isNull();
        assertThat(result.lifetimeCashSaved()).isNull();
        assertThat(result.comparisonUnavailableReason()).contains("NON_AMORTIZING");
    }

    @Test
    void bothHorizonLimitSuppressesLifetimeComparison() {
        DebtPrepaymentComparisonResult result = DebtPrepaymentComparisonCalculator.calculate(
                new BigDecimal("1000000.00"), new BigDecimal("0.001"), new BigDecimal("1005.00"), "USD",
                new BigDecimal("0.00"));

        assertThat(result.baseline().status()).isEqualTo(DebtAmortizationStatus.HORIZON_LIMIT);
        assertThat(result.scenario().status()).isEqualTo(DebtAmortizationStatus.HORIZON_LIMIT);
        assertThat(result.lifetimeInterestSaved()).isNull();
        assertThat(result.payoffMonthsSaved()).isNull();
        assertThat(result.lifetimeCashSaved()).isNull();
        assertThat(result.comparisonUnavailableReason()).contains("HORIZON_LIMIT");
    }

    @Test
    void prepaymentThatMakesOnlyScenarioRepayableWithinHorizonSuppressesLifetimeComparison() {
        DebtPrepaymentComparisonResult result = DebtPrepaymentComparisonCalculator.calculate(
                new BigDecimal("1000000.00"), new BigDecimal("0.001"), new BigDecimal("1005.00"), "USD",
                new BigDecimal("999000.00"));

        assertThat(result.baseline().status()).isEqualTo(DebtAmortizationStatus.HORIZON_LIMIT);
        assertThat(result.scenario().status()).isEqualTo(DebtAmortizationStatus.PAID_OFF);
        assertThat(result.lifetimeInterestSaved()).isNull();
        assertThat(result.payoffMonthsSaved()).isNull();
        assertThat(result.lifetimeCashSaved()).isNull();
        assertThat(result.comparisonUnavailableReason()).contains("HORIZON_LIMIT");
    }

    @Test
    void normalizesLowercaseCurrencyToUppercase() {
        DebtPrepaymentComparisonResult result = DebtPrepaymentComparisonCalculator.calculate(
                new BigDecimal("1000.00"), new BigDecimal("0"), new BigDecimal("300.00"), "usd",
                new BigDecimal("400.00"));

        assertThat(result.currency()).isEqualTo("USD");
        assertThat(result.baseline().currency()).isEqualTo("USD");
        assertThat(result.scenario().currency()).isEqualTo("USD");
    }
}
