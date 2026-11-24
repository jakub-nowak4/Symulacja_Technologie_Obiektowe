package lab3.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TicksTest {
    private static final double DELTA = 1e-12;

    @Test
    void perSecondIsTwentyFive() {
        assertEquals(25, Ticks.PER_SECOND);
    }

    @Test
    void dtIsFortyMilliseconds() {
        assertEquals(0.04, Ticks.DT, DELTA);
    }

    @Test
    void maxSpeedMovesTenCentimetersPerTick() {
        assertEquals(0.1, 2.5 * Ticks.DT, DELTA);
    }

    @Test
    void fromSecondsConvertsSpecificationConstants() {
        assertEquals(75, Ticks.fromSeconds(3));
        assertEquals(500, Ticks.fromSeconds(20));
        assertEquals(750, Ticks.fromSeconds(30));
    }

    @Test
    void fromSecondsOfZeroIsZero() {
        assertEquals(0, Ticks.fromSeconds(0));
    }

    @Test
    void fromSecondsRoundsToNearestTick() {
        assertEquals(0, Ticks.fromSeconds(0.01));
        assertEquals(1, Ticks.fromSeconds(0.02));
        assertEquals(1, Ticks.fromSeconds(0.04));
        assertEquals(2, Ticks.fromSeconds(0.07));
    }

    @Test
    void fromSecondsHandlesLargeDurations() {
        assertEquals(6_000_000L, Ticks.fromSeconds(240_000));
    }

    @Test
    void fromSecondsRejectsNegativeValue() {
        assertThrows(IllegalArgumentException.class, () -> Ticks.fromSeconds(-1));
    }

    @Test
    void fromSecondsRejectsNaN() {
        assertThrows(IllegalArgumentException.class, () -> Ticks.fromSeconds(Double.NaN));
    }

    @Test
    void fromSecondsRejectsInfinity() {
        assertThrows(IllegalArgumentException.class, () -> Ticks.fromSeconds(Double.POSITIVE_INFINITY));
    }

    @Test
    void toSecondsConvertsTicks() {
        assertEquals(3.0, Ticks.toSeconds(75), DELTA);
        assertEquals(0.04, Ticks.toSeconds(1), DELTA);
        assertEquals(0.0, Ticks.toSeconds(0), DELTA);
    }

    @Test
    void roundTripThroughSecondsKeepsWholeTicks() {
        for (long ticks = 0; ticks <= 2000; ticks++) {
            assertEquals(ticks, Ticks.fromSeconds(Ticks.toSeconds(ticks)));
        }
    }
}
