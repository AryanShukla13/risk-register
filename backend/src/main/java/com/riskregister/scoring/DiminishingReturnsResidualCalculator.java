package com.riskregister.scoring;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Each mitigation of effectiveness e (1..5) removes 15% x e of the risk. Mitigations combine
 * multiplicatively (each reduces what remains), total reduction is capped at 90%, and the
 * result is rounded half-up and never drops below 1.
 *
 * <p>Integer percentages are used so rounding is exact.
 */
@Component
public class DiminishingReturnsResidualCalculator implements ResidualScoreCalculator {

    static final int PERCENT_PER_EFFECTIVENESS_POINT = 15;
    static final int MIN_REMAINING_PERCENT = 10;

    @Override
    public int residual(int inherentScore, List<Integer> mitigationEffectiveness) {
        if (inherentScore < SeverityBand.MIN_SCORE || inherentScore > SeverityBand.MAX_SCORE) {
            throw new IllegalArgumentException("Inherent score out of range: " + inherentScore);
        }
        // remaining expressed as a fraction of 100^n, kept in BigDecimal to stay exact
        BigDecimal remaining = BigDecimal.ONE;
        for (int e : mitigationEffectiveness) {
            RiskScoring.requireScale("effectiveness", e);
            BigDecimal keep = BigDecimal.valueOf(100 - PERCENT_PER_EFFECTIVENESS_POINT * e)
                    .movePointLeft(2);
            remaining = remaining.multiply(keep);
        }
        BigDecimal floor = BigDecimal.valueOf(MIN_REMAINING_PERCENT).movePointLeft(2);
        remaining = remaining.max(floor);

        int residual = BigDecimal.valueOf(inherentScore)
                .multiply(remaining)
                .setScale(0, RoundingMode.HALF_UP)
                .intValueExact();
        return Math.max(1, Math.min(residual, inherentScore));
    }
}
