package lab3.core;

import lab3.vectors.Vector2D;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContactTrackerTest {
    private static final long THRESHOLD = SimulationConfig.CONTACT_TICKS;

    private static Person person(int id, double x, double y) {
        return Person.healthy(id, new Motion(new Vector2D(x, y), new Vector2D(0, 0)));
    }

    private static Person infectious(int id, double x, double y, boolean symptomatic) {
        Person p = person(id, x, y);
        p.infect(symptomatic, 1000);
        return p;
    }

    private static Person immune(int id, double x, double y) {
        return new Person(id, new Motion(new Vector2D(x, y), new Vector2D(0, 0)),
                lab3.core.health.ImmuneState.INSTANCE);
    }

    private static List<Infection> evaluateTimes(ContactTracker tracker, List<Person> people, Rng rng, long times) {
        List<Infection> last = List.of();
        for (long i = 0; i < times; i++) {
            last = tracker.evaluate(people, rng);
            if (i < times - 1) {
                assertTrue(last.isEmpty(), "unexpected infection at tick " + (i + 1));
            }
        }
        return last;
    }

    @Test
    void thresholdIsThreeSecondsOfTicks() {
        assertEquals(75, THRESHOLD);
    }

    @Test
    void noOneInContactMeansNoInfectionsAndNoDraws() {
        ScriptedRng rng = new ScriptedRng(0.0);
        List<Person> people = List.of(person(1, 0, 0), infectious(2, 10, 10, true));

        assertTrue(new ContactTracker().evaluate(people, rng).isEmpty());
        assertEquals(0, rng.state());
    }

    @Test
    void seventyFourTicksInContactAreNotEnoughAndSeventyFiveAre() {
        ContactTracker tracker = new ContactTracker();
        ScriptedRng rng = new ScriptedRng(0.0);
        List<Person> people = List.of(person(1, 0, 0), infectious(2, 1, 0, true));

        for (int i = 0; i < THRESHOLD - 1; i++) {
            assertTrue(tracker.evaluate(people, rng).isEmpty(), "too early at tick " + (i + 1));
        }
        assertEquals(0, rng.state());

        List<Infection> infections = tracker.evaluate(people, rng);

        assertEquals(List.of(new Infection(1, 2)), infections);
        assertEquals(1, rng.state());
    }

    @Test
    void distanceOfExactlyTwoMetersCountsAsContact() {
        List<Person> people = List.of(person(1, 0, 0), infectious(2, 2.0, 0, true));

        List<Infection> infections = evaluateTimes(new ContactTracker(), people, new ScriptedRng(0.0), THRESHOLD);

        assertEquals(1, infections.size());
    }

    @Test
    void justOverTwoMetersIsNoContact() {
        ContactTracker tracker = new ContactTracker();
        ScriptedRng rng = new ScriptedRng(0.0);
        List<Person> people = List.of(person(1, 0, 0), infectious(2, 2.000001, 0, true));

        for (int i = 0; i < 500; i++) {
            assertTrue(tracker.evaluate(people, rng).isEmpty());
        }
        assertEquals(0, rng.state());
    }

    @Test
    void leavingTheRadiusResetsTheCounter() {
        ContactTracker tracker = new ContactTracker();
        ScriptedRng rng = new ScriptedRng(0.0);
        Person target = person(1, 0, 0);
        Person source = infectious(2, 1.9, 0, true);
        List<Person> people = List.of(target, source);

        for (int i = 0; i < THRESHOLD - 1; i++) {
            assertTrue(tracker.evaluate(people, rng).isEmpty());
        }
        source.setMotion(new Motion(new Vector2D(2.1, 0), new Vector2D(0, 0)));
        assertTrue(tracker.evaluate(people, rng).isEmpty());
        source.setMotion(new Motion(new Vector2D(1.9, 0), new Vector2D(0, 0)));

        for (int i = 0; i < THRESHOLD - 1; i++) {
            assertTrue(tracker.evaluate(people, rng).isEmpty(), "counter was not reset, tick " + (i + 1));
        }
        assertEquals(1, tracker.evaluate(people, rng).size());
    }

    @Test
    void asymptomaticSourceTransmitsWhenTheDrawIsBelowHalf() {
        List<Person> people = List.of(person(1, 0, 0), infectious(2, 1, 0, false));

        List<Infection> infections = evaluateTimes(new ContactTracker(), people, new ScriptedRng(0.49), THRESHOLD);

        assertEquals(List.of(new Infection(1, 2)), infections);
    }

    @Test
    void asymptomaticSourceDoesNotTransmitWhenTheDrawIsHalfOrMore() {
        List<Person> people = List.of(person(1, 0, 0), infectious(2, 1, 0, false));

        List<Infection> infections = evaluateTimes(new ContactTracker(), people, new ScriptedRng(0.50), THRESHOLD);

        assertTrue(infections.isEmpty());
    }

    @Test
    void symptomaticSourceAlwaysTransmitsEvenForTheHighestDraw() {
        List<Person> people = List.of(person(1, 0, 0), infectious(2, 1, 0, true));

        List<Infection> infections = evaluateTimes(new ContactTracker(), people,
                new ScriptedRng(Math.nextDown(1.0)), THRESHOLD);

        assertEquals(1, infections.size());
    }

    @Test
    void afterAFailedDrawThePairIsNotDrawnAgainWhileContactContinues() {
        ContactTracker tracker = new ContactTracker();
        ScriptedRng rng = new ScriptedRng(0.9);
        List<Person> people = List.of(person(1, 0, 0), infectious(2, 1, 0, false));

        evaluateTimes(tracker, people, rng, THRESHOLD);
        assertEquals(1, rng.state());

        for (int i = 0; i < 1000; i++) {
            assertTrue(tracker.evaluate(people, rng).isEmpty());
        }
        assertEquals(1, rng.state());
    }

    @Test
    void afterBreakingTheContactThePairIsDrawnAgain() {
        ContactTracker tracker = new ContactTracker();
        ScriptedRng rng = new ScriptedRng(0.9, 0.9);
        Person source = infectious(2, 1, 0, false);
        List<Person> people = List.of(person(1, 0, 0), source);
        evaluateTimes(tracker, people, rng, THRESHOLD);

        source.setMotion(new Motion(new Vector2D(5, 0), new Vector2D(0, 0)));
        tracker.evaluate(people, rng);
        source.setMotion(new Motion(new Vector2D(1, 0), new Vector2D(0, 0)));
        evaluateTimes(tracker, people, rng, THRESHOLD);

        assertEquals(2, rng.state());
    }

    @Test
    void aSuccessfulPairIsNotDrawnAgainWhileTheCallerKeepsTheSameStates() {
        ContactTracker tracker = new ContactTracker();
        ScriptedRng rng = new ScriptedRng(0.1);
        List<Person> people = List.of(person(1, 0, 0), infectious(2, 1, 0, false));
        evaluateTimes(tracker, people, rng, THRESHOLD);

        for (int i = 0; i < 100; i++) {
            assertTrue(tracker.evaluate(people, rng).isEmpty());
        }
        assertEquals(1, rng.state());
    }

    @Test
    void immuneAndAlreadyInfectedPeopleAreNeverTargets() {
        ContactTracker tracker = new ContactTracker();
        ScriptedRng rng = new ScriptedRng(0.0);
        List<Person> people = List.of(
                immune(1, 0, 0),
                infectious(2, 0.5, 0, true),
                infectious(3, 1, 0, false));

        for (int i = 0; i < 300; i++) {
            assertTrue(tracker.evaluate(people, rng).isEmpty());
        }
        assertEquals(0, rng.state());
    }

    @Test
    void healthyPeopleDoNotInfectEachOther() {
        ContactTracker tracker = new ContactTracker();
        ScriptedRng rng = new ScriptedRng(0.0);
        List<Person> people = List.of(person(1, 0, 0), person(2, 0.5, 0), person(3, 1, 0));

        for (int i = 0; i < 300; i++) {
            assertTrue(tracker.evaluate(people, rng).isEmpty());
        }
        assertEquals(0, rng.state());
    }

    @Test
    void aTargetWithTwoSourcesReachingTheThresholdTogetherIsInfectedOnceByTheLowestId() {
        ContactTracker tracker = new ContactTracker();
        ScriptedRng rng = new ScriptedRng(0.0, 0.0);
        List<Person> people = List.of(person(1, 0, 0), infectious(9, 1, 0, true), infectious(4, 0, 1, true));

        List<Infection> infections = evaluateTimes(tracker, people, rng, THRESHOLD);

        assertEquals(List.of(new Infection(1, 4)), infections);
        assertEquals(2, rng.state());
    }

    @Test
    void oneSourceCanInfectSeveralTargetsInTheSameTick() {
        List<Person> people = List.of(person(1, 0, 0), person(2, 0.5, 0), infectious(3, 1, 0, true));

        List<Infection> infections = evaluateTimes(new ContactTracker(), people, new ScriptedRng(0.0, 0.0), THRESHOLD);

        assertEquals(List.of(new Infection(1, 3), new Infection(2, 3)), infections);
    }

    @Test
    void aSourceThatDisappearsForOneTickResetsItsContacts() {
        ContactTracker tracker = new ContactTracker();
        ScriptedRng rng = new ScriptedRng(0.0);
        Person target = person(1, 0, 0);
        Person source = infectious(2, 1, 0, true);

        for (int i = 0; i < THRESHOLD - 1; i++) {
            assertTrue(tracker.evaluate(List.of(target, source), rng).isEmpty());
        }
        assertTrue(tracker.evaluate(List.of(target), rng).isEmpty());

        for (int i = 0; i < THRESHOLD - 1; i++) {
            assertTrue(tracker.evaluate(List.of(target, source), rng).isEmpty());
        }
        assertEquals(1, tracker.evaluate(List.of(target, source), rng).size());
    }

    @Test
    void theResultDoesNotDependOnTheOrderOfThePeopleList() {
        List<Person> people = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            double x = (i % 4) * 0.8;
            double y = (i / 4) * 0.8;
            people.add(i % 3 == 0 ? infectious(i, x, y, i % 2 == 0) : person(i, x, y));
        }
        List<Person> shuffled = new ArrayList<>(people);
        Collections.shuffle(shuffled, new Random(3));
        ContactTracker a = new ContactTracker();
        ContactTracker b = new ContactTracker();
        Rng rngA = new SplitMix64Rng(11);
        Rng rngB = new SplitMix64Rng(11);
        List<Infection> allA = new ArrayList<>();
        List<Infection> allB = new ArrayList<>();

        for (int i = 0; i < 200; i++) {
            allA.addAll(a.evaluate(people, rngA));
            allB.addAll(b.evaluate(shuffled, rngB));
        }

        assertEquals(allA, allB);
        assertEquals(rngA.state(), rngB.state());
        assertTrue(!allA.isEmpty(), "the scenario should produce infections");
    }

    @Test
    void symptomaticSourceTransmitsInEveryTrial() {
        Rng rng = new SplitMix64Rng(1);
        int successes = 0;

        for (int trial = 0; trial < 500; trial++) {
            List<Person> people = List.of(person(1, 0, 0), infectious(2, 1, 0, true));
            if (!evaluateTimes(new ContactTracker(), people, rng, THRESHOLD).isEmpty()) {
                successes++;
            }
        }

        assertEquals(500, successes);
    }

    @Test
    void asymptomaticSourceTransmitsInAboutHalfOfTheTrials() {
        Rng rng = new SplitMix64Rng(2026);
        int trials = 20_000;
        int successes = 0;

        for (int trial = 0; trial < trials; trial++) {
            List<Person> people = List.of(person(1, 0, 0), infectious(2, 1, 0, false));
            ContactTracker tracker = new ContactTracker();
            List<Infection> infections = List.of();
            for (int tick = 0; tick < THRESHOLD; tick++) {
                infections = tracker.evaluate(people, rng);
            }
            if (!infections.isEmpty()) {
                successes++;
            }
        }

        assertEquals(0.5, (double) successes / trials, 0.015);
    }

    @Test
    void negativeIdsAreRejected() {
        List<Person> people = List.of(person(-1, 0, 0), infectious(2, 1, 0, true));

        assertThrows(IllegalArgumentException.class, () -> new ContactTracker().evaluate(people, new ScriptedRng(0.0)));
    }
}
