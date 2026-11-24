package lab3.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SplitMix64RngTest {

    @Test
    void matchesReferenceSequenceForSeedZero() {
        SplitMix64Rng rng = new SplitMix64Rng(0);

        assertEquals(0xE220A8397B1DCDAFL, rng.nextLong());
        assertEquals(0x6E789E6AA1B965F4L, rng.nextLong());
        assertEquals(0x06C45D188009454FL, rng.nextLong());
    }

    @Test
    void sameSeedGivesSameSequence() {
        Rng a = new SplitMix64Rng(42);
        Rng b = new SplitMix64Rng(42);

        for (int i = 0; i < 1000; i++) {
            assertEquals(a.nextDouble(), b.nextDouble(), 0.0);
        }
    }

    @Test
    void differentSeedsGiveDifferentSequences() {
        Rng a = new SplitMix64Rng(1);
        Rng b = new SplitMix64Rng(2);

        assertNotEquals(a.nextDouble(), b.nextDouble());
    }

    @Test
    void nextDoubleStaysInZeroInclusiveOneExclusive() {
        Rng rng = new SplitMix64Rng(7);

        for (int i = 0; i < 100_000; i++) {
            double v = rng.nextDouble();
            assertTrue(v >= 0.0 && v < 1.0, "out of range: " + v);
        }
    }

    @Test
    void nextDoubleMeanIsCloseToHalf() {
        Rng rng = new SplitMix64Rng(123);
        double sum = 0;
        int n = 200_000;

        for (int i = 0; i < n; i++) {
            sum += rng.nextDouble();
        }

        assertEquals(0.5, sum / n, 0.005);
    }

    @Test
    void stateReproducesTheSequenceOnTheSameInstance() {
        Rng rng = new SplitMix64Rng(99);
        for (int i = 0; i < 10; i++) {
            rng.nextDouble();
        }
        long saved = rng.state();
        double[] first = new double[10];
        for (int i = 0; i < 10; i++) {
            first[i] = rng.nextDouble();
        }

        rng.setState(saved);

        for (int i = 0; i < 10; i++) {
            assertEquals(first[i], rng.nextDouble(), 0.0);
        }
    }

    @Test
    void stateReproducesTheSequenceOnAFreshInstance() {
        Rng original = new SplitMix64Rng(5);
        for (int i = 0; i < 25; i++) {
            original.nextDouble();
        }
        Rng fresh = new SplitMix64Rng(0);
        fresh.setState(original.state());

        for (int i = 0; i < 100; i++) {
            assertEquals(original.nextDouble(), fresh.nextDouble(), 0.0);
        }
    }

    @Test
    void nextIntStaysInBoundsAndCoversAllValues() {
        Rng rng = new SplitMix64Rng(11);
        boolean[] seen = new boolean[5];

        for (int i = 0; i < 10_000; i++) {
            int v = rng.nextInt(5);
            assertTrue(v >= 0 && v < 5, "out of range: " + v);
            seen[v] = true;
        }

        for (boolean s : seen) {
            assertTrue(s);
        }
    }

    @Test
    void nextIntRejectsNonPositiveBound() {
        Rng rng = new SplitMix64Rng(1);

        assertThrows(IllegalArgumentException.class, () -> rng.nextInt(0));
        assertThrows(IllegalArgumentException.class, () -> rng.nextInt(-3));
    }

    @Test
    void chanceZeroIsNeverTrueAndChanceOneIsAlwaysTrue() {
        Rng rng = new SplitMix64Rng(3);

        for (int i = 0; i < 1000; i++) {
            assertFalse(rng.chance(0.0));
            assertTrue(rng.chance(1.0));
        }
    }

    @Test
    void chanceHalfIsTrueAboutHalfOfTheTime() {
        Rng rng = new SplitMix64Rng(2024);
        int hits = 0;
        int n = 100_000;

        for (int i = 0; i < n; i++) {
            if (rng.chance(0.5)) {
                hits++;
            }
        }

        assertEquals(0.5, (double) hits / n, 0.01);
    }

    @Test
    void chanceRejectsProbabilityOutsideZeroOne() {
        Rng rng = new SplitMix64Rng(1);

        assertThrows(IllegalArgumentException.class, () -> rng.chance(-0.1));
        assertThrows(IllegalArgumentException.class, () -> rng.chance(1.1));
        assertThrows(IllegalArgumentException.class, () -> rng.chance(Double.NaN));
    }

    @Test
    void rangeStaysInsideBounds() {
        Rng rng = new SplitMix64Rng(8);

        for (int i = 0; i < 10_000; i++) {
            double v = rng.range(-2.0, 3.0);
            assertTrue(v >= -2.0 && v < 3.0, "out of range: " + v);
        }
    }

    @Test
    void rangeWithEqualBoundsReturnsThatValue() {
        assertEquals(1.5, new SplitMix64Rng(1).range(1.5, 1.5), 0.0);
    }

    @Test
    void rangeRejectsReversedBounds() {
        Rng rng = new SplitMix64Rng(1);

        assertThrows(IllegalArgumentException.class, () -> rng.range(2.0, 1.0));
    }

    @Test
    void everyHelperConsumesExactlyOneDraw() {
        long seed = 77;
        Rng reference = new SplitMix64Rng(seed);
        reference.nextDouble();
        long afterOneDraw = reference.state();

        Rng a = new SplitMix64Rng(seed);
        a.chance(0.3);
        Rng b = new SplitMix64Rng(seed);
        b.range(1.0, 2.0);
        Rng c = new SplitMix64Rng(seed);
        c.nextInt(10);
        Rng d = new SplitMix64Rng(seed);
        d.chance(0.0);

        assertEquals(afterOneDraw, a.state());
        assertEquals(afterOneDraw, b.state());
        assertEquals(afterOneDraw, c.state());
        assertEquals(afterOneDraw, d.state());
    }
}
