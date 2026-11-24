package lab3.core.health;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HealthStateTest {
    private static final double DELTA = 1e-12;

    @Test
    void healthyIsSusceptibleAndNotInfectious() {
        HealthState s = HealthyState.INSTANCE;

        assertEquals(HealthKind.HEALTHY, s.kind());
        assertTrue(s.isSusceptible());
        assertFalse(s.isInfectious());
        assertEquals(0.0, s.transmissionProbability(), DELTA);
    }

    @Test
    void immuneIsNeitherSusceptibleNorInfectious() {
        HealthState s = ImmuneState.INSTANCE;

        assertEquals(HealthKind.IMMUNE, s.kind());
        assertFalse(s.isSusceptible());
        assertFalse(s.isInfectious());
        assertEquals(0.0, s.transmissionProbability(), DELTA);
    }

    @Test
    void asymptomaticSpreadsWithProbabilityHalf() {
        HealthState s = InfectedAsymptomaticState.INSTANCE;

        assertEquals(HealthKind.INFECTED_ASYMPTOMATIC, s.kind());
        assertFalse(s.isSusceptible());
        assertTrue(s.isInfectious());
        assertEquals(0.5, s.transmissionProbability(), DELTA);
    }

    @Test
    void symptomaticAlwaysSpreads() {
        HealthState s = InfectedSymptomaticState.INSTANCE;

        assertEquals(HealthKind.INFECTED_SYMPTOMATIC, s.kind());
        assertFalse(s.isSusceptible());
        assertTrue(s.isInfectious());
        assertEquals(1.0, s.transmissionProbability(), DELTA);
    }

    @Test
    void healthyBecomesInfectedWithOrWithoutSymptoms() {
        assertSame(InfectedSymptomaticState.INSTANCE, HealthyState.INSTANCE.infect(true));
        assertSame(InfectedAsymptomaticState.INSTANCE, HealthyState.INSTANCE.infect(false));
    }

    @Test
    void immuneCannotBeInfected() {
        assertSame(ImmuneState.INSTANCE, ImmuneState.INSTANCE.infect(true));
        assertSame(ImmuneState.INSTANCE, ImmuneState.INSTANCE.infect(false));
    }

    @Test
    void infectedCannotBeInfectedAgainNorChangeSymptoms() {
        assertSame(InfectedAsymptomaticState.INSTANCE, InfectedAsymptomaticState.INSTANCE.infect(true));
        assertSame(InfectedSymptomaticState.INSTANCE, InfectedSymptomaticState.INSTANCE.infect(false));
    }

    @Test
    void healthyAndImmuneDoNotChangeOverTime() {
        for (long ticksLeft : new long[]{-1, 0, 1, 750}) {
            assertSame(HealthyState.INSTANCE, HealthyState.INSTANCE.afterTick(ticksLeft));
            assertSame(ImmuneState.INSTANCE, ImmuneState.INSTANCE.afterTick(ticksLeft));
        }
    }

    @Test
    void infectedStayInfectedWhileTimeRemains() {
        assertSame(InfectedAsymptomaticState.INSTANCE, InfectedAsymptomaticState.INSTANCE.afterTick(1));
        assertSame(InfectedSymptomaticState.INSTANCE, InfectedSymptomaticState.INSTANCE.afterTick(750));
    }

    @Test
    void infectedRecoverIntoImmunityWhenTimeRunsOut() {
        assertSame(ImmuneState.INSTANCE, InfectedAsymptomaticState.INSTANCE.afterTick(0));
        assertSame(ImmuneState.INSTANCE, InfectedSymptomaticState.INSTANCE.afterTick(0));
        assertSame(ImmuneState.INSTANCE, InfectedSymptomaticState.INSTANCE.afterTick(-3));
    }

    @Test
    void ofReturnsTheSharedInstanceForEveryKind() {
        assertSame(HealthyState.INSTANCE, HealthState.of(HealthKind.HEALTHY));
        assertSame(ImmuneState.INSTANCE, HealthState.of(HealthKind.IMMUNE));
        assertSame(InfectedAsymptomaticState.INSTANCE, HealthState.of(HealthKind.INFECTED_ASYMPTOMATIC));
        assertSame(InfectedSymptomaticState.INSTANCE, HealthState.of(HealthKind.INFECTED_SYMPTOMATIC));
    }

    @Test
    void stateAndKindAgreeAndFollowTheHierarchy() {
        for (HealthKind kind : HealthKind.values()) {
            HealthState state = HealthState.of(kind);

            assertEquals(kind, state.kind());
            assertEquals(kind.isInfected(), state.isInfectious());
            assertEquals(kind == HealthKind.HEALTHY, state.isSusceptible());
            assertFalse(state.isSusceptible() && state.isInfectious());
        }
    }
}
