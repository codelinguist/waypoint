package com.waypoint.assumption;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.waypoint.household.Household;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PlanningAssumptionTest {

    private final Household household = new Household("Ralph Household", "PHP");

    @Test
    void isActiveWhenAsOfIsWithinOpenEndedWindowAndNotSuperseded() {
        PlanningAssumption assumption = new PlanningAssumption(
                household, "Expected annual return", "0.06", "PERCENTAGE_ANNUAL", null,
                LocalDate.of(2026, 1, 1), null, LocalDate.of(2027, 1, 1));

        assertThat(assumption.isActiveAsOf(LocalDate.of(2030, 6, 1))).isTrue();
    }

    @Test
    void isNotActiveBeforeEffectiveFrom() {
        PlanningAssumption assumption = new PlanningAssumption(
                household, "Expected annual return", "0.06", "PERCENTAGE_ANNUAL", null,
                LocalDate.of(2026, 6, 1), null, LocalDate.of(2027, 1, 1));

        assertThat(assumption.isActiveAsOf(LocalDate.of(2026, 5, 31))).isFalse();
    }

    @Test
    void isActiveOnExactEffectiveFromDate() {
        PlanningAssumption assumption = new PlanningAssumption(
                household, "Expected annual return", "0.06", "PERCENTAGE_ANNUAL", null,
                LocalDate.of(2026, 6, 1), null, LocalDate.of(2027, 1, 1));

        assertThat(assumption.isActiveAsOf(LocalDate.of(2026, 6, 1))).isTrue();
    }

    @Test
    void isActiveOnExactEffectiveUntilDate() {
        PlanningAssumption assumption = new PlanningAssumption(
                household, "Expected annual return", "0.06", "PERCENTAGE_ANNUAL", null,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), LocalDate.of(2027, 1, 1));

        assertThat(assumption.isActiveAsOf(LocalDate.of(2026, 12, 31))).isTrue();
    }

    @Test
    void isNotActiveAfterEffectiveUntil() {
        PlanningAssumption assumption = new PlanningAssumption(
                household, "Expected annual return", "0.06", "PERCENTAGE_ANNUAL", null,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), LocalDate.of(2027, 1, 1));

        assertThat(assumption.isActiveAsOf(LocalDate.of(2027, 1, 1))).isFalse();
    }

    @Test
    void isNotActiveWhenSupersededRegardlessOfTemporalWindow() {
        PlanningAssumption assumption = new PlanningAssumption(
                household, "Expected annual return", "0.06", "PERCENTAGE_ANNUAL", null,
                LocalDate.of(2026, 1, 1), null, LocalDate.of(2027, 1, 1));
        PlanningAssumption replacement = new PlanningAssumption(
                household, "Expected annual return", "0.07", "PERCENTAGE_ANNUAL", null,
                LocalDate.of(2026, 1, 1), null, LocalDate.of(2027, 1, 1));

        assumption.linkSupersededBy(replacement);

        assertThat(assumption.isActiveAsOf(LocalDate.of(2026, 6, 1))).isFalse();
    }

    @Test
    void linkSupersededByRecordsTheReplacement() {
        PlanningAssumption assumption = new PlanningAssumption(
                household, "Expected annual return", "0.06", "PERCENTAGE_ANNUAL", null,
                LocalDate.of(2026, 1, 1), null, LocalDate.of(2027, 1, 1));
        PlanningAssumption replacement = new PlanningAssumption(
                household, "Expected annual return", "0.07", "PERCENTAGE_ANNUAL", null,
                LocalDate.of(2026, 1, 1), null, LocalDate.of(2027, 1, 1));

        assumption.linkSupersededBy(replacement);

        assertThat(assumption.getSupersededBy()).isSameAs(replacement);
    }

    @Test
    void cannotBeSupersededTwice() {
        PlanningAssumption assumption = new PlanningAssumption(
                household, "Expected annual return", "0.06", "PERCENTAGE_ANNUAL", null,
                LocalDate.of(2026, 1, 1), null, LocalDate.of(2027, 1, 1));
        PlanningAssumption firstReplacement = new PlanningAssumption(
                household, "Expected annual return", "0.07", "PERCENTAGE_ANNUAL", null,
                LocalDate.of(2026, 1, 1), null, LocalDate.of(2027, 1, 1));
        PlanningAssumption secondReplacement = new PlanningAssumption(
                household, "Expected annual return", "0.08", "PERCENTAGE_ANNUAL", null,
                LocalDate.of(2026, 1, 1), null, LocalDate.of(2027, 1, 1));
        assumption.linkSupersededBy(firstReplacement);

        assertThatThrownBy(() -> assumption.linkSupersededBy(secondReplacement))
                .isInstanceOf(IllegalStateException.class);
    }
}
