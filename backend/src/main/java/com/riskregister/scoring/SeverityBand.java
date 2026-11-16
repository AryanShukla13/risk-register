package com.riskregister.scoring;

/** Display band for an inherent or residual score (1..25). */
public enum SeverityBand {
    LOW(1, 5), MEDIUM(6, 12), HIGH(13, 19), CRITICAL(20, 25);

    public static final int MIN_SCORE = 1;
    public static final int MAX_SCORE = 25;

    private final int min;
    private final int max;

    SeverityBand(int min, int max) {
        this.min = min;
        this.max = max;
    }

    public static SeverityBand of(int score) {
        for (SeverityBand band : values()) {
            if (score >= band.min && score <= band.max) {
                return band;
            }
        }
        throw new IllegalArgumentException(
                "Score must be between %d and %d but was %d".formatted(MIN_SCORE, MAX_SCORE, score));
    }
}
