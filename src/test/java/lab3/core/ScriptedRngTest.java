package lab3.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScriptedRngTest {

    @Test
    void returnsValuesInOrder() {
        Rng rng = new ScriptedRng(0.1, 0.2, 0.3);

        assertEquals(0.1, rng.nextDouble(), 0.0);
        assertEquals(0.2, rng.nextDouble(), 0.0);
        assertEquals(0.3, rng.nextDouble(), 0.0);
    }

    @Test
    void failsWhenValuesRunOut() {
        Rng rng = new ScriptedRng(0.5);
        rng.nextDouble();

        assertThrows(IllegalStateException.class, rng::nextDouble);
    }

    @Test
    void repeatingLastKeepsReturningTheLastValue() {
        Rng rng = ScriptedRng.repeatingLast(0.1, 0.9);

        assertEquals(0.1, rng.nextDouble(), 0.0);
        assertEquals(0.9, rng.nextDouble(), 0.0);
        assertEquals(0.9, rng.nextDouble(), 0.0);
        assertEquals(0.9, rng.nextDouble(), 0.0);
    }

    @Test
    void stateCanBeSavedAndRestored() {
        Rng rng = new ScriptedRng(0.1, 0.2, 0.3);
        rng.nextDouble();
        long saved = rng.state();
        double expected = rng.nextDouble();

        rng.setState(saved);

        assertEquals(expected, rng.nextDouble(), 0.0);
    }

    @Test
    void worksWithChanceThresholds() {
        assertTrue(new ScriptedRng(0.49).chance(0.5));
        assertFalse(new ScriptedRng(0.50).chance(0.5));
    }

    @Test
    void rejectsEmptyOrOutOfRangeValues() {
        assertThrows(IllegalArgumentException.class, ScriptedRng::new);
        assertThrows(IllegalArgumentException.class, () -> new ScriptedRng(1.0));
        assertThrows(IllegalArgumentException.class, () -> new ScriptedRng(-0.1));
    }
}
