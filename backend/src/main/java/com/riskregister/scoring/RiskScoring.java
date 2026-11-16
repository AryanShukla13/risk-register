package com.riskregister.scoring;

/** Pure scoring helpers. No framework dependencies. */
public final class RiskScoring {

    private RiskScoring() {}

    /** Inherent score = likelihood x impact (1..25). */
    public static int inherent(int likelihood, int impact) {
        requireScale("likelihood", likelihood);
        requireScale("impact", impact);
        return likelihood * impact;
    }

    static void requireScale(String name, int value) {
        if (value < 1 || value > 5) {
            throw new IllegalArgumentException(name + " must be between 1 and 5 but was " + value);
        }
    }
}
