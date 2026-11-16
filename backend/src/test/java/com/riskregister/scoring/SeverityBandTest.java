package com.riskregister.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class SeverityBandTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "1,LOW", "5,LOW",
            "6,MEDIUM", "12,MEDIUM",
            "13,HIGH", "19,HIGH",
            "20,CRITICAL", "25,CRITICAL"})
    void bandBoundaries(int score, SeverityBand expected) {
        assertThat(SeverityBand.of(score)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 26, 100})
    void outOfRangeIsRejected(int score) {
        assertThatThrownBy(() -> SeverityBand.of(score)).isInstanceOf(IllegalArgumentException.class);
    }

    @org.junit.jupiter.api.Test
    void everyScoreFrom1To25HasExactlyOneBand() {
        for (int s = 1; s <= 25; s++) {
            assertThat(SeverityBand.of(s)).isNotNull();
        }
    }
}
