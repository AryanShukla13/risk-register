package com.riskregister.scoring;

import java.util.List;

/** Strategy for turning an inherent score and mitigation effectiveness values into a residual score. */
public interface ResidualScoreCalculator {

    /**
     * @param inherentScore            1..25
     * @param mitigationEffectiveness  one 1..5 value per mitigation; may be empty
     * @return residual score, between 1 and {@code inherentScore}
     */
    int residual(int inherentScore, List<Integer> mitigationEffectiveness);
}
