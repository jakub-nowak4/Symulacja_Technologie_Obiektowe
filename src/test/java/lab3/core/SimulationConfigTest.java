package lab3.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SimulationConfigTest {
    private static final double DELTA = 1e-12;

    private static SimulationConfig base() {
        return SimulationConfig.noImmunity();
    }

    private static SimulationConfig with(double width, double height, int population, int infected,
                                         double entryRate, double minSpeed, double immune,
                                         double symptomatic, double changeProbability,
                                         double maxTurn, double maxSpeedDelta) {
        return new SimulationConfig(width, height, population, infected, entryRate, minSpeed, immune,
                symptomatic, changeProbability, maxTurn, maxSpeedDelta);
    }

    @Test
    void specificationConstantsMatchTheAssignment() {
        assertEquals(2.5, SimulationConfig.MAX_SPEED, DELTA);
        assertEquals(2.0, SimulationConfig.CONTACT_RADIUS, DELTA);
        assertEquals(75, SimulationConfig.CONTACT_TICKS);
        assertEquals(500, SimulationConfig.INFECTION_MIN_TICKS);
        assertEquals(750, SimulationConfig.INFECTION_MAX_TICKS);
        assertEquals(0.1, SimulationConfig.ENTRY_INFECTED_PROBABILITY, DELTA);
        assertEquals(0.5, SimulationConfig.BOUNCE_PROBABILITY, DELTA);
        assertEquals(0.5, SimulationConfig.TRANSMISSION_ASYMPTOMATIC, DELTA);
        assertEquals(1.0, SimulationConfig.TRANSMISSION_SYMPTOMATIC, DELTA);
    }

    @Test
    void noImmunityPresetHasNoImmuneAndIsValid() {
        SimulationConfig c = SimulationConfig.noImmunity();

        assertEquals(0.0, c.immuneProbability(), DELTA);
        assertEquals(0, c.initialInfected());
        assertEquals(60, c.initialPopulation());
    }

    @Test
    void withImmunityPresetChangesOnlyImmuneProbability() {
        SimulationConfig c = SimulationConfig.withImmunity(0.3);

        assertEquals(0.3, c.immuneProbability(), DELTA);
        assertEquals(SimulationConfig.noImmunity().withImmuneProbability(0.3), c);
        assertEquals(base().areaWidth(), c.areaWidth(), DELTA);
        assertEquals(base().entryRatePerSecond(), c.entryRatePerSecond(), DELTA);
    }

    @Test
    void withImmuneProbabilityReturnsACopy() {
        SimulationConfig original = base();

        SimulationConfig changed = original.withImmuneProbability(0.5);

        assertEquals(0.0, original.immuneProbability(), DELTA);
        assertNotEquals(original, changed);
    }

    @Test
    void entryProbabilityPerTickIsRateTimesDt() {
        SimulationConfig c = with(30, 30, 60, 0, 2.5, 0.3, 0, 0.5, 0.02, 0.5, 0.5);

        assertEquals(0.1, c.entryProbabilityPerTick(), DELTA);
    }

    @Test
    void oneEntryPerTickIsTheLargestAllowedRate() {
        assertDoesNotThrow(() -> with(30, 30, 60, 0, 25.0, 0.3, 0, 0.5, 0.02, 0.5, 0.5));
        assertThrows(IllegalArgumentException.class,
                () -> with(30, 30, 60, 0, 25.01, 0.3, 0, 0.5, 0.02, 0.5, 0.5));
    }

    @Test
    void rejectsInvalidAreaSize() {
        assertThrows(IllegalArgumentException.class, () -> with(0, 30, 60, 0, 1, 0.3, 0, 0.5, 0.02, 0.5, 0.5));
        assertThrows(IllegalArgumentException.class, () -> with(30, -1, 60, 0, 1, 0.3, 0, 0.5, 0.02, 0.5, 0.5));
        assertThrows(IllegalArgumentException.class,
                () -> with(Double.NaN, 30, 60, 0, 1, 0.3, 0, 0.5, 0.02, 0.5, 0.5));
        assertThrows(IllegalArgumentException.class,
                () -> with(30, Double.POSITIVE_INFINITY, 60, 0, 1, 0.3, 0, 0.5, 0.02, 0.5, 0.5));
    }

    @Test
    void rejectsInvalidPopulationNumbers() {
        assertThrows(IllegalArgumentException.class, () -> with(30, 30, -1, 0, 1, 0.3, 0, 0.5, 0.02, 0.5, 0.5));
        assertThrows(IllegalArgumentException.class, () -> with(30, 30, 10, -1, 1, 0.3, 0, 0.5, 0.02, 0.5, 0.5));
        assertThrows(IllegalArgumentException.class, () -> with(30, 30, 10, 11, 1, 0.3, 0, 0.5, 0.02, 0.5, 0.5));
        assertDoesNotThrow(() -> with(30, 30, 10, 10, 1, 0.3, 0, 0.5, 0.02, 0.5, 0.5));
    }

    @Test
    void rejectsInvalidEntryRate() {
        assertThrows(IllegalArgumentException.class, () -> with(30, 30, 60, 0, -0.1, 0.3, 0, 0.5, 0.02, 0.5, 0.5));
        assertThrows(IllegalArgumentException.class,
                () -> with(30, 30, 60, 0, Double.NaN, 0.3, 0, 0.5, 0.02, 0.5, 0.5));
        assertDoesNotThrow(() -> with(30, 30, 60, 0, 0.0, 0.3, 0, 0.5, 0.02, 0.5, 0.5));
    }

    @Test
    void rejectsInvalidMinSpeed() {
        assertThrows(IllegalArgumentException.class, () -> with(30, 30, 60, 0, 1, 0.0, 0, 0.5, 0.02, 0.5, 0.5));
        assertThrows(IllegalArgumentException.class, () -> with(30, 30, 60, 0, 1, 2.6, 0, 0.5, 0.02, 0.5, 0.5));
        assertDoesNotThrow(() -> with(30, 30, 60, 0, 1, 2.5, 0, 0.5, 0.02, 0.5, 0.5));
    }

    @Test
    void rejectsProbabilitiesOutsideZeroOne() {
        assertThrows(IllegalArgumentException.class, () -> with(30, 30, 60, 0, 1, 0.3, -0.1, 0.5, 0.02, 0.5, 0.5));
        assertThrows(IllegalArgumentException.class, () -> with(30, 30, 60, 0, 1, 0.3, 1.1, 0.5, 0.02, 0.5, 0.5));
        assertThrows(IllegalArgumentException.class, () -> with(30, 30, 60, 0, 1, 0.3, 0, 1.5, 0.02, 0.5, 0.5));
        assertThrows(IllegalArgumentException.class, () -> with(30, 30, 60, 0, 1, 0.3, 0, 0.5, -0.5, 0.5, 0.5));
        assertThrows(IllegalArgumentException.class,
                () -> with(30, 30, 60, 0, 1, 0.3, Double.NaN, 0.5, 0.02, 0.5, 0.5));
        assertDoesNotThrow(() -> with(30, 30, 60, 0, 1, 0.3, 1.0, 1.0, 1.0, 0.5, 0.5));
    }

    @Test
    void rejectsInvalidMotionChangeLimits() {
        assertThrows(IllegalArgumentException.class, () -> with(30, 30, 60, 0, 1, 0.3, 0, 0.5, 0.02, -0.1, 0.5));
        assertThrows(IllegalArgumentException.class, () -> with(30, 30, 60, 0, 1, 0.3, 0, 0.5, 0.02, 3.2, 0.5));
        assertThrows(IllegalArgumentException.class, () -> with(30, 30, 60, 0, 1, 0.3, 0, 0.5, 0.02, 0.5, -0.1));
        assertDoesNotThrow(() -> with(30, 30, 60, 0, 1, 0.3, 0, 0.5, 0.02, 0.0, 0.0));
    }
}
