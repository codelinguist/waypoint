package com.waypoint.position.web.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Formats domain {@link BigDecimal} money values as exact two-decimal, non-
 * exponential strings for this endpoint's browser-facing contract, so a
 * JavaScript {@code Number} consumer can never silently lose cents. Scoped to
 * this package's response DTOs only; existing endpoints keep serializing
 * money as JSON numbers via their own DTOs, unchanged.
 *
 * <p>{@link RoundingMode#UNNECESSARY} is deliberate: every value reaching this
 * endpoint already carries scale 2 or less (column precision or exact
 * BigDecimal summation), so padding to scale 2 never requires rounding. If
 * that ever stopped being true, failing loudly here is correct: silently
 * rounding would misstate an exact monetary figure.
 */
final class MoneyFormat {

    private MoneyFormat() {
    }

    static String plain(BigDecimal value) {
        return value.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }
}
