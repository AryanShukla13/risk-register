package com.riskregister.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DiminishingReturnsResidualCalculatorTest {

    private final DiminishingReturnsResidualCalculator calc = new DiminishingReturnsResidualCalculator();

    static List<Arguments> knownCases() {
        return List.of(
                Arguments.of(20, List.of(), 20),            // no mitigations: residual == inherent
                Arguments.of(25, List.of(), 25),
                Arguments.of(20, List.of(5), 5),            // one strong control: 20 * 0.25
                Arguments.of(20, List.of(3, 3), 6),         // 20 * 0.55 * 0.55 = 6.05
                Arguments.of(25, List.of(5, 5, 5), 3),      // capped at 90% reduction: 2.5 -> 3
                Arguments.of(1, List.of(5), 1),             // never below 1
                Arguments.of(3, List.of(1), 3),             // 2.55 rounds up to 3
                Arguments.of(10, List.of(1), 9),            // 10 * 0.85 = 8.5 -> half-up 9
                Arguments.of(10, List.of(2), 7));           // 10 * 0.70
    }

    @ParameterizedTest(name = "inherent {0} with {1} -> {2}")
    @MethodSource("knownCases")
    void knownValues(int inherent, List<Integer> effectiveness, int expected) {
        assertThat(calc.residual(inherent, effectiveness)).isEqualTo(expected);
    }

    @Test
    void oneHighlyEffectiveMitigationIsMeaningfullyLower() {
        assertThat(calc.residual(20, List.of(5))).isLessThanOrEqualTo(10);
    }

    @Test
    void neverBelowOneAndNeverAboveInherent() {
        for (int inherent = 1; inherent <= 25; inherent++) {
            for (int e1 = 1; e1 <= 5; e1++) {
                for (int e2 = 1; e2 <= 5; e2++) {
                    int r = calc.residual(inherent, List.of(e1, e2));
                    assertThat(r).isBetween(1, inherent);
                }
            }
        }
    }

    @Test
    void addingAMitigationNeverIncreasesResidual() {
        for (int inherent = 1; inherent <= 25; inherent++) {
            List<Integer> controls = new ArrayList<>();
            int previous = calc.residual(inherent, controls);
            for (int e : new int[] {1, 3, 2, 5, 4}) {
                controls.add(e);
                int next = calc.residual(inherent, controls);
                assertThat(next).isLessThanOrEqualTo(previous);
                previous = next;
            }
        }
    }

    @Test
    void orderOfMitigationsDoesNotMatter() {
        List<Integer> a = new ArrayList<>(List.of(1, 4, 2, 5));
        int expected = calc.residual(18, a);
        Collections.reverse(a);
        assertThat(calc.residual(18, a)).isEqualTo(expected);
    }

    @Test
    void totalReductionIsCappedAt90Percent() {
        // many perfect controls cannot push 25 below 2.5 -> 3
        assertThat(calc.residual(25, Collections.nCopies(10, 5))).isEqualTo(3);
    }

    @Test
    void rejectsInvalidInputs() {
        assertThatThrownBy(() -> calc.residual(0, List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calc.residual(26, List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calc.residual(10, List.of(6))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calc.residual(10, List.of(0))).isInstanceOf(IllegalArgumentException.class);
    }
}
