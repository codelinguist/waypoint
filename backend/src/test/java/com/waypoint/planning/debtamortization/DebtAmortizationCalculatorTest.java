package com.waypoint.planning.debtamortization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class DebtAmortizationCalculatorTest {

    @Test
    void payoffInFourMonthsWithZeroInterestMatchesWorkedExample() {
        DebtAmortizationResult result = DebtAmortizationCalculator.calculate(
                new BigDecimal("1000.00"), new BigDecimal("0"), new BigDecimal("300.00"), "USD");

        assertThat(result.status()).isEqualTo(DebtAmortizationStatus.PAID_OFF);
        assertThat(result.payoffMonths()).isEqualTo(4);
        assertThat(result.schedule()).hasSize(4);
        assertThat(paymentsOf(result)).containsExactly(
                new BigDecimal("300.00"), new BigDecimal("300.00"), new BigDecimal("300.00"), new BigDecimal("100.00"));
        assertThat(result.totalPaid()).isEqualByComparingTo("1000.00");
        assertThat(result.totalInterest()).isEqualByComparingTo("0.00");
        assertReconciles(result);
    }

    @Test
    void payoffWithInterestMatchesSecondWorkedExample() {
        DebtAmortizationResult result = DebtAmortizationCalculator.calculate(
                new BigDecimal("100.00"), new BigDecimal("0.01"), new BigDecimal("60.00"), "USD");

        assertThat(result.status()).isEqualTo(DebtAmortizationStatus.PAID_OFF);
        assertThat(result.payoffMonths()).isEqualTo(2);
        assertThat(result.schedule()).hasSize(2);

        DebtAmortizationRow firstRow = result.schedule().get(0);
        assertThat(firstRow.interest()).isEqualByComparingTo("1.00");
        assertThat(firstRow.closingBalance()).isEqualByComparingTo("41.00");

        DebtAmortizationRow secondRow = result.schedule().get(1);
        assertThat(secondRow.interest()).isEqualByComparingTo("0.41");
        assertThat(secondRow.payment()).isEqualByComparingTo("41.41");
        assertThat(secondRow.closingBalance()).isEqualByComparingTo("0.00");

        assertThat(result.totalInterest()).isEqualByComparingTo("1.41");
        assertThat(result.totalPaid()).isEqualByComparingTo("101.41");
        assertReconciles(result);
    }

    @Test
    void halfCentRoundingRoundsHalfUpNotDownOrEven() {
        // 1.00 * 0.005 = 0.005, an actual half-cent: HALF_UP rounds to 0.01, while
        // HALF_DOWN and HALF_EVEN would both round to 0.00.
        DebtAmortizationResult result = DebtAmortizationCalculator.calculate(
                new BigDecimal("1.00"), new BigDecimal("0.005"), new BigDecimal("2.00"), "USD");

        assertThat(result.status()).isEqualTo(DebtAmortizationStatus.PAID_OFF);
        assertThat(result.payoffMonths()).isEqualTo(1);
        assertThat(result.schedule()).hasSize(1);

        DebtAmortizationRow row = result.schedule().get(0);
        assertThat(row.interest()).isEqualByComparingTo("0.01");
        assertThat(row.payment()).isEqualByComparingTo("1.01");
        assertThat(row.principalRepaid()).isEqualByComparingTo("1.00");
        assertThat(row.closingBalance()).isEqualByComparingTo("0.00");

        assertThat(result.totalInterest()).isEqualByComparingTo("0.01");
        assertThat(result.totalPaid()).isEqualByComparingTo("1.01");
        assertReconciles(result);
    }

    @Test
    void zeroPrincipalReturnsPaidOffWithNoRows() {
        DebtAmortizationResult result = DebtAmortizationCalculator.calculate(
                new BigDecimal("0"), new BigDecimal("0.01"), new BigDecimal("10.00"), "USD");

        assertThat(result.status()).isEqualTo(DebtAmortizationStatus.PAID_OFF);
        assertThat(result.payoffMonths()).isEqualTo(0);
        assertThat(result.schedule()).isEmpty();
        assertThat(result.totalPaid()).isEqualByComparingTo("0.00");
        assertThat(result.totalInterest()).isEqualByComparingTo("0.00");
        assertThat(result.remainingBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void paymentEqualToFirstInterestReturnsNonAmortizing() {
        DebtAmortizationResult result = DebtAmortizationCalculator.calculate(
                new BigDecimal("1000.00"), new BigDecimal("0.01"), new BigDecimal("10.00"), "USD");

        assertThat(result.status()).isEqualTo(DebtAmortizationStatus.NON_AMORTIZING);
        assertThat(result.payoffMonths()).isNull();
        assertThat(result.schedule()).isEmpty();
        assertThat(result.totalPaid()).isEqualByComparingTo("0.00");
        assertThat(result.totalInterest()).isEqualByComparingTo("0.00");
        assertThat(result.remainingBalance()).isEqualByComparingTo("1000.00");
    }

    @Test
    void paymentBelowFirstInterestReturnsNonAmortizing() {
        DebtAmortizationResult result = DebtAmortizationCalculator.calculate(
                new BigDecimal("1000.00"), new BigDecimal("0.01"), new BigDecimal("5.00"), "USD");

        assertThat(result.status()).isEqualTo(DebtAmortizationStatus.NON_AMORTIZING);
        assertThat(result.payoffMonths()).isNull();
        assertThat(result.schedule()).isEmpty();
    }

    @Test
    void payoffExactlyAtHorizonIsStillPaidOff() {
        DebtAmortizationResult result = DebtAmortizationCalculator.calculate(
                new BigDecimal("1200.00"), new BigDecimal("0"), new BigDecimal("1.00"), "USD");

        assertThat(result.status()).isEqualTo(DebtAmortizationStatus.PAID_OFF);
        assertThat(result.payoffMonths()).isEqualTo(DebtAmortizationCalculator.MAX_HORIZON_MONTHS);
        assertThat(result.schedule()).hasSize(DebtAmortizationCalculator.MAX_HORIZON_MONTHS);
        assertThat(result.remainingBalance()).isEqualByComparingTo("0.00");
        assertReconciles(result);
    }

    @Test
    void stillPositiveBalanceAtHorizonReturnsHorizonLimitWithPartialTotals() {
        DebtAmortizationResult result = DebtAmortizationCalculator.calculate(
                new BigDecimal("1000000.00"), new BigDecimal("0.001"), new BigDecimal("1005.00"), "USD");

        assertThat(result.status()).isEqualTo(DebtAmortizationStatus.HORIZON_LIMIT);
        assertThat(result.payoffMonths()).isNull();
        assertThat(result.schedule()).hasSize(DebtAmortizationCalculator.MAX_HORIZON_MONTHS);
        assertThat(result.remainingBalance()).isGreaterThan(BigDecimal.ZERO);
        assertThat(result.totalPaid()).isGreaterThan(BigDecimal.ZERO);
        assertThat(result.totalInterest()).isGreaterThan(BigDecimal.ZERO);
        assertReconciles(result);
    }

    @Test
    void everyRowReconcilesAndNeverOverpaysOrGoesNegative() {
        DebtAmortizationResult result = DebtAmortizationCalculator.calculate(
                new BigDecimal("5000.00"), new BigDecimal("0.015"), new BigDecimal("150.00"), "USD");

        assertReconciles(result);
        for (DebtAmortizationRow row : result.schedule()) {
            assertThat(row.closingBalance()).isGreaterThanOrEqualTo(BigDecimal.ZERO.setScale(2));
            assertThat(row.payment()).isLessThanOrEqualTo(row.openingBalance().add(row.interest()));
        }
    }

    @Test
    void normalizesLowercaseCurrencyToUppercase() {
        DebtAmortizationResult result = DebtAmortizationCalculator.calculate(
                new BigDecimal("1000.00"), new BigDecimal("0"), new BigDecimal("300.00"), "usd");

        assertThat(result.currency()).isEqualTo("USD");
    }

    @Test
    void rejectsNullPrincipal() {
        assertThatThrownBy(() -> DebtAmortizationCalculator.calculate(
                null, new BigDecimal("0.01"), new BigDecimal("10.00"), "USD"))
                .isInstanceOf(InvalidDebtAmortizationInputException.class);
    }

    @Test
    void rejectsNullRate() {
        assertThatThrownBy(() -> DebtAmortizationCalculator.calculate(
                new BigDecimal("1000.00"), null, new BigDecimal("10.00"), "USD"))
                .isInstanceOf(InvalidDebtAmortizationInputException.class);
    }

    @Test
    void rejectsNullPayment() {
        assertThatThrownBy(() -> DebtAmortizationCalculator.calculate(
                new BigDecimal("1000.00"), new BigDecimal("0.01"), null, "USD"))
                .isInstanceOf(InvalidDebtAmortizationInputException.class);
    }

    @Test
    void rejectsNullCurrency() {
        assertThatThrownBy(() -> DebtAmortizationCalculator.calculate(
                new BigDecimal("1000.00"), new BigDecimal("0.01"), new BigDecimal("10.00"), null))
                .isInstanceOf(InvalidDebtAmortizationInputException.class);
    }

    @Test
    void rejectsNegativePrincipal() {
        assertThatThrownBy(() -> DebtAmortizationCalculator.calculate(
                new BigDecimal("-1.00"), new BigDecimal("0.01"), new BigDecimal("10.00"), "USD"))
                .isInstanceOf(InvalidDebtAmortizationInputException.class);
    }

    @Test
    void rejectsZeroPayment() {
        assertThatThrownBy(() -> DebtAmortizationCalculator.calculate(
                new BigDecimal("1000.00"), new BigDecimal("0.01"), new BigDecimal("0.00"), "USD"))
                .isInstanceOf(InvalidDebtAmortizationInputException.class);
    }

    @Test
    void rejectsNegativePayment() {
        assertThatThrownBy(() -> DebtAmortizationCalculator.calculate(
                new BigDecimal("1000.00"), new BigDecimal("0.01"), new BigDecimal("-10.00"), "USD"))
                .isInstanceOf(InvalidDebtAmortizationInputException.class);
    }

    @Test
    void rejectsNegativeRate() {
        assertThatThrownBy(() -> DebtAmortizationCalculator.calculate(
                new BigDecimal("1000.00"), new BigDecimal("-0.01"), new BigDecimal("10.00"), "USD"))
                .isInstanceOf(InvalidDebtAmortizationInputException.class);
    }

    @Test
    void rejectsRateAboveOne() {
        assertThatThrownBy(() -> DebtAmortizationCalculator.calculate(
                new BigDecimal("1000.00"), new BigDecimal("1.01"), new BigDecimal("10.00"), "USD"))
                .isInstanceOf(InvalidDebtAmortizationInputException.class);
    }

    @Test
    void rejectsRateWithExcessiveFractionalDigits() {
        assertThatThrownBy(() -> DebtAmortizationCalculator.calculate(
                new BigDecimal("1000.00"), new BigDecimal("0.123456789"), new BigDecimal("10.00"), "USD"))
                .isInstanceOf(InvalidDebtAmortizationInputException.class);
    }

    @Test
    void rejectsPrincipalWithExcessiveFractionalScale() {
        assertThatThrownBy(() -> DebtAmortizationCalculator.calculate(
                new BigDecimal("1000.005"), new BigDecimal("0.01"), new BigDecimal("10.00"), "USD"))
                .isInstanceOf(InvalidDebtAmortizationInputException.class);
    }

    @Test
    void rejectsPrincipalWithExcessiveIntegerDigits() {
        assertThatThrownBy(() -> DebtAmortizationCalculator.calculate(
                new BigDecimal("123456789012345678.00"), new BigDecimal("0.01"), new BigDecimal("10.00"), "USD"))
                .isInstanceOf(InvalidDebtAmortizationInputException.class);
    }

    @Test
    void rejectsMonthlyPaymentWithExcessiveFractionalScale() {
        assertThatThrownBy(() -> DebtAmortizationCalculator.calculate(
                new BigDecimal("1000.00"), new BigDecimal("0.01"), new BigDecimal("10.005"), "USD"))
                .isInstanceOf(InvalidDebtAmortizationInputException.class);
    }

    @Test
    void rejectsMonthlyPaymentWithExcessiveIntegerDigits() {
        assertThatThrownBy(() -> DebtAmortizationCalculator.calculate(
                new BigDecimal("1000.00"), new BigDecimal("0.01"), new BigDecimal("123456789012345678.00"), "USD"))
                .isInstanceOf(InvalidDebtAmortizationInputException.class);
    }

    @Test
    void rejectsBlankCurrency() {
        assertThatThrownBy(() -> DebtAmortizationCalculator.calculate(
                new BigDecimal("1000.00"), new BigDecimal("0.01"), new BigDecimal("10.00"), "  "))
                .isInstanceOf(InvalidDebtAmortizationInputException.class);
    }

    @Test
    void rejectsMalformedCurrency() {
        assertThatThrownBy(() -> DebtAmortizationCalculator.calculate(
                new BigDecimal("1000.00"), new BigDecimal("0.01"), new BigDecimal("10.00"), "US1"))
                .isInstanceOf(InvalidDebtAmortizationInputException.class);
    }

    private static java.util.List<BigDecimal> paymentsOf(DebtAmortizationResult result) {
        return result.schedule().stream().map(DebtAmortizationRow::payment).toList();
    }

    private static void assertReconciles(DebtAmortizationResult result) {
        BigDecimal sumInterest = BigDecimal.ZERO.setScale(2);
        BigDecimal sumPayment = BigDecimal.ZERO.setScale(2);
        BigDecimal previousClosing = result.principal();
        for (DebtAmortizationRow row : result.schedule()) {
            assertThat(row.openingBalance()).isEqualByComparingTo(previousClosing);
            assertThat(row.openingBalance().add(row.interest()).subtract(row.payment()))
                    .isEqualByComparingTo(row.closingBalance());
            assertThat(row.closingBalance()).isGreaterThanOrEqualTo(BigDecimal.ZERO.setScale(2));
            sumInterest = sumInterest.add(row.interest());
            sumPayment = sumPayment.add(row.payment());
            previousClosing = row.closingBalance();
        }
        assertThat(sumInterest).isEqualByComparingTo(result.totalInterest());
        assertThat(sumPayment).isEqualByComparingTo(result.totalPaid());
    }
}
