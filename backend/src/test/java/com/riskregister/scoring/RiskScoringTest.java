package com.riskregister.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RiskScoringTest {

    @Test
    void inherentIsLikelihoodTimesImpactAcrossTheWholeGrid() {
        for (int l = 1; l <= 5; l++) {
            for (int i = 1; i <= 5; i++) {
                int score = RiskScoring.inherent(l, i);
                assertThat(score).isEqualTo(l * i).isBetween(1, 25);
            }
        }
    }

    @ParameterizedTest
    @CsvSource({"0,3", "6,3", "3,0", "3,6", "-1,1"})
    void inherentRejectsOutOfScaleInputs(int likelihood, int impact) {
        assertThatThrownBy(() -> RiskScoring.inherent(likelihood, impact))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
