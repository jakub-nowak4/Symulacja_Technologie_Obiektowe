package lab3.core;

import lab3.core.health.HealthKind;
import lab3.vectors.Vector2D;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InfectionPolicyTest {
    private static final InfectionPolicy POLICY = new InfectionPolicy(SimulationConfig.noImmunity());

    private static Person healthy() {
        return Person.healthy(1, new Motion(new Vector2D(0, 0), new Vector2D(0, 0)));
    }

    @Test
    void lowestDrawsGiveSymptomsAndTheShortestInfection() {
        Person p = healthy();

        assertTrue(POLICY.infect(p, new ScriptedRng(0.0, 0.0)));

        assertEquals(HealthKind.INFECTED_SYMPTOMATIC, p.kind());
        assertEquals(500, p.infectionTicksLeft());
    }

    @Test
    void highestDrawsGiveNoSymptomsAndTheLongestInfection() {
        Person p = healthy();

        POLICY.infect(p, new ScriptedRng(Math.nextDown(1.0), Math.nextDown(1.0)));

        assertEquals(HealthKind.INFECTED_ASYMPTOMATIC, p.kind());
        assertEquals(750, p.infectionTicksLeft());
    }

    @Test
    void symptomsDrawHasThresholdAtTheConfiguredProbability() {
        Person withSymptoms = healthy();
        Person without = healthy();

        POLICY.infect(withSymptoms, new ScriptedRng(0.49, 0.5));
        POLICY.infect(without, new ScriptedRng(0.50, 0.5));

        assertEquals(HealthKind.INFECTED_SYMPTOMATIC, withSymptoms.kind());
        assertEquals(HealthKind.INFECTED_ASYMPTOMATIC, without.kind());
    }

    @Test
    void alwaysUsesTwoDrawsEvenIfThePersonCannotBeInfected() {
        ScriptedRng rng = new ScriptedRng(0.1, 0.2, 0.3, 0.4);
        Person p = healthy();
        POLICY.infect(p, rng);

        assertFalse(POLICY.infect(p, rng));

        assertEquals(4, rng.state());
        assertEquals(HealthKind.INFECTED_SYMPTOMATIC, p.kind());
    }

    @Test
    void durationsCoverTheWholeRangeAndNeverLeaveIt() {
        Rng rng = new SplitMix64Rng(4);
        boolean sawMin = false;
        boolean sawMax = false;

        for (int i = 0; i < 200_000; i++) {
            Person p = healthy();
            POLICY.infect(p, rng);
            long d = p.infectionTicksLeft();
            assertTrue(d >= 500 && d <= 750, "duration out of range: " + d);
            sawMin |= d == 500;
            sawMax |= d == 750;
        }

        assertTrue(sawMin && sawMax);
    }

    @Test
    void aboutHalfOfNewInfectionsHaveSymptoms() {
        Rng rng = new SplitMix64Rng(9);
        int n = 50_000;
        int symptomatic = 0;

        for (int i = 0; i < n; i++) {
            Person p = healthy();
            POLICY.infect(p, rng);
            if (p.kind() == HealthKind.INFECTED_SYMPTOMATIC) {
                symptomatic++;
            }
        }

        assertEquals(0.5, (double) symptomatic / n, 0.01);
    }
}
