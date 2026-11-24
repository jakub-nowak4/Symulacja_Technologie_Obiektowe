package lab3.core;

import lab3.core.health.HealthKind;
import lab3.core.health.HealthyState;
import lab3.core.health.ImmuneState;
import lab3.core.health.InfectedAsymptomaticState;
import lab3.core.health.InfectedSymptomaticState;
import lab3.core.view.PersonView;
import lab3.vectors.Vector2D;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersonTest {
    private static final double DELTA = 1e-12;

    private static Motion motion() {
        return new Motion(new Vector2D(3, 4), new Vector2D(1, 0));
    }

    private static Person healthy() {
        return Person.healthy(7, motion());
    }

    @Test
    void newHealthyPersonIsSusceptibleAndNotInfected() {
        Person p = healthy();

        assertEquals(7, p.id());
        assertEquals(HealthKind.HEALTHY, p.kind());
        assertTrue(p.isSusceptible());
        assertFalse(p.isInfectious());
        assertEquals(0, p.infectionTicksLeft());
    }

    @Test
    void infectingAHealthyPersonSucceeds() {
        Person p = healthy();

        assertTrue(p.infect(true, 600));

        assertEquals(HealthKind.INFECTED_SYMPTOMATIC, p.kind());
        assertTrue(p.isInfectious());
        assertFalse(p.isSusceptible());
        assertEquals(600, p.infectionTicksLeft());
    }

    @Test
    void infectionWithoutSymptomsGivesTheAsymptomaticState() {
        Person p = healthy();

        p.infect(false, 500);

        assertSame(InfectedAsymptomaticState.INSTANCE, p.state());
    }

    @Test
    void immunePersonCannotBeInfected() {
        Person p = new Person(1, motion(), ImmuneState.INSTANCE);

        assertFalse(p.infect(true, 600));

        assertEquals(HealthKind.IMMUNE, p.kind());
        assertEquals(0, p.infectionTicksLeft());
    }

    @Test
    void infectedPersonCannotBeInfectedAgainAndKeepsItsClock() {
        Person p = healthy();
        p.infect(false, 600);
        p.advanceHealth();

        assertFalse(p.infect(true, 750));

        assertEquals(HealthKind.INFECTED_ASYMPTOMATIC, p.kind());
        assertEquals(599, p.infectionTicksLeft());
    }

    @Test
    void infectionLastsExactlyTheGivenNumberOfTicks() {
        for (long duration : new long[]{1, 3, 500, 750}) {
            Person p = healthy();
            p.infect(true, duration);

            for (long i = 1; i < duration; i++) {
                p.advanceHealth();
                assertTrue(p.isInfectious(), "recovered too early at tick " + i + " of " + duration);
            }
            p.advanceHealth();

            assertEquals(HealthKind.IMMUNE, p.kind());
            assertEquals(0, p.infectionTicksLeft());
        }
    }

    @Test
    void infectionTicksLeftCountsDown() {
        Person p = healthy();
        p.infect(true, 10);

        p.advanceHealth();
        p.advanceHealth();

        assertEquals(8, p.infectionTicksLeft());
    }

    @Test
    void recoveredPersonStaysImmuneAndCannotBeInfectedAgain() {
        Person p = healthy();
        p.infect(true, 2);
        p.advanceHealth();
        p.advanceHealth();

        assertFalse(p.infect(true, 600));

        assertEquals(HealthKind.IMMUNE, p.kind());
        p.advanceHealth();
        assertEquals(HealthKind.IMMUNE, p.kind());
    }

    @Test
    void advancingTimeDoesNotChangeHealthyOrImmunePeople() {
        Person healthy = healthy();
        Person immune = new Person(2, motion(), ImmuneState.INSTANCE);

        for (int i = 0; i < 1000; i++) {
            healthy.advanceHealth();
            immune.advanceHealth();
        }

        assertEquals(HealthKind.HEALTHY, healthy.kind());
        assertEquals(HealthKind.IMMUNE, immune.kind());
    }

    @Test
    void rejectsNonPositiveInfectionDuration() {
        Person p = healthy();

        assertThrows(IllegalArgumentException.class, () -> p.infect(true, 0));
        assertThrows(IllegalArgumentException.class, () -> p.infect(true, -5));
        assertEquals(HealthKind.HEALTHY, p.kind());
    }

    @Test
    void restoringAnInfectedPersonKeepsTheClock() {
        Person p = new Person(3, motion(), InfectedSymptomaticState.INSTANCE, 42);

        assertEquals(HealthKind.INFECTED_SYMPTOMATIC, p.kind());
        assertEquals(42, p.infectionTicksLeft());
    }

    @Test
    void constructorRejectsInconsistentClockAndState() {
        assertThrows(IllegalArgumentException.class,
                () -> new Person(1, motion(), InfectedSymptomaticState.INSTANCE, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new Person(1, motion(), HealthyState.INSTANCE, 5));
        assertThrows(IllegalArgumentException.class,
                () -> new Person(1, motion(), ImmuneState.INSTANCE, 1));
    }

    @Test
    void constructorRejectsNulls() {
        assertThrows(IllegalArgumentException.class, () -> new Person(1, null, HealthyState.INSTANCE));
        assertThrows(IllegalArgumentException.class, () -> new Person(1, motion(), null));
    }

    @Test
    void setMotionReplacesTheMotion() {
        Person p = healthy();
        Motion moved = new Motion(new Vector2D(9, 9), new Vector2D(0, 1));

        p.setMotion(moved);

        assertSame(moved, p.motion());
        assertThrows(IllegalArgumentException.class, () -> p.setMotion(null));
    }

    @Test
    void viewReflectsIdPositionAndHealth() {
        Person p = healthy();
        p.infect(false, 100);

        PersonView view = p.view();

        assertEquals(7, view.id());
        assertEquals(3.0, view.x(), DELTA);
        assertEquals(4.0, view.y(), DELTA);
        assertEquals(HealthKind.INFECTED_ASYMPTOMATIC, view.kind());
    }
}
