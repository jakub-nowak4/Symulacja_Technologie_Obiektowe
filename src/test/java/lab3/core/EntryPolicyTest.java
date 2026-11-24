package lab3.core;

import lab3.core.health.HealthKind;
import lab3.vectors.Vector2D;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntryPolicyTest {
    private static final double DELTA = 1e-9;

    private static SimulationConfig config(int population, int infected, double entryRate, double immune) {
        return new SimulationConfig(30, 30, population, infected, entryRate, 0.3, immune, 0.5, 0.02, Math.PI / 4, 0.5);
    }

    private static AtomicInteger counter() {
        return new AtomicInteger();
    }

    private static boolean onBorder(Vector2D p, Area area) {
        return p.getX() == 0 || p.getX() == area.width() || p.getY() == 0 || p.getY() == area.height();
    }

    @Test
    void initialPopulationHasTheConfiguredSizeAndConsecutiveIds() {
        EntryPolicy policy = new EntryPolicy(SimulationConfig.noImmunity());
        AtomicInteger ids = counter();

        List<Person> people = policy.createInitialPopulation(new SplitMix64Rng(1), ids::getAndIncrement);

        assertEquals(60, people.size());
        for (int i = 0; i < people.size(); i++) {
            assertEquals(i, people.get(i).id());
        }
        assertEquals(60, ids.get());
    }

    @Test
    void initialPeopleAreInsideTheAreaWithAllowedSpeeds() {
        SimulationConfig config = SimulationConfig.noImmunity();
        Area area = new Area(config.areaWidth(), config.areaHeight());

        for (Person p : new EntryPolicy(config).createInitialPopulation(new SplitMix64Rng(2), counter()::getAndIncrement)) {
            assertTrue(area.contains(p.motion().position()));
            assertTrue(p.motion().speed() >= config.minSpeed() - DELTA);
            assertTrue(p.motion().speed() <= SimulationConfig.MAX_SPEED + DELTA);
        }
    }

    @Test
    void scenarioWithoutImmunityStartsEveryoneHealthy() {
        List<Person> people = new EntryPolicy(SimulationConfig.noImmunity())
                .createInitialPopulation(new SplitMix64Rng(3), counter()::getAndIncrement);

        for (Person p : people) {
            assertEquals(HealthKind.HEALTHY, p.kind());
        }
    }

    @Test
    void scenarioWithImmunityMakesTheConfiguredShareImmune() {
        EntryPolicy policy = new EntryPolicy(config(20_000, 0, 1.8, 0.3));

        List<Person> people = policy.createInitialPopulation(new SplitMix64Rng(4), counter()::getAndIncrement);

        long immune = people.stream().filter(p -> p.kind() == HealthKind.IMMUNE).count();
        assertEquals(0.3, (double) immune / people.size(), 0.015);
        assertTrue(people.stream().allMatch(p -> p.kind() == HealthKind.IMMUNE || p.kind() == HealthKind.HEALTHY));
    }

    @Test
    void theFirstInitialInfectedPeopleAreInfectedAndNeverImmune() {
        EntryPolicy policy = new EntryPolicy(config(10, 4, 1.8, 1.0));

        List<Person> people = policy.createInitialPopulation(new SplitMix64Rng(5), counter()::getAndIncrement);

        for (int i = 0; i < 4; i++) {
            assertTrue(people.get(i).kind().isInfected(), "person " + i);
            assertTrue(people.get(i).infectionTicksLeft() >= 500 && people.get(i).infectionTicksLeft() <= 750);
        }
        for (int i = 4; i < 10; i++) {
            assertEquals(HealthKind.IMMUNE, people.get(i).kind());
        }
    }

    @Test
    void nobodyEntersWhenTheDrawIsAboveTheEntryProbability() {
        EntryPolicy policy = new EntryPolicy(SimulationConfig.noImmunity());   // 1.8 per second = 0.072 per tick
        ScriptedRng rng = new ScriptedRng(0.08);
        AtomicInteger ids = counter();

        Optional<Person> entrant = policy.maybeEnter(rng, ids::getAndIncrement);

        assertTrue(entrant.isEmpty());
        assertEquals(1, rng.state());
        assertEquals(0, ids.get());
    }

    @Test
    void someoneEntersWhenTheDrawIsBelowTheEntryProbability() {
        EntryPolicy policy = new EntryPolicy(SimulationConfig.noImmunity());
        AtomicInteger ids = new AtomicInteger(41);
        ScriptedRng rng = new ScriptedRng(0.071, 0.05, 0.5, 0.25, 0.9, 0.5);

        Person p = policy.maybeEnter(rng, ids::getAndIncrement).orElseThrow();

        assertEquals(41, p.id());
        assertEquals(42, ids.get());
    }

    @Test
    void scriptedHealthyEntrantStartsOnTheBorderMovingInwards() {
        EntryPolicy policy = new EntryPolicy(SimulationConfig.noImmunity());
        // enter, border point (t = 0.05 * 120 = 6 -> bottom side), speed 1.4, heading pi/2, not immune, not infected
        ScriptedRng rng = new ScriptedRng(0.0, 0.05, 0.5, 0.25, 0.9, 0.5);

        Person p = policy.maybeEnter(rng, counter()::getAndIncrement).orElseThrow();

        assertEquals(6.0, p.motion().position().getX(), DELTA);
        assertEquals(0.0, p.motion().position().getY(), DELTA);
        assertEquals(0.0, p.motion().velocity().getX(), DELTA);
        assertEquals(1.4, p.motion().velocity().getY(), DELTA);
        assertEquals(HealthKind.HEALTHY, p.kind());
        assertEquals(6, rng.state());
    }

    @Test
    void scriptedEntrantMovingOutwardsIsTurnedInwards() {
        EntryPolicy policy = new EntryPolicy(SimulationConfig.noImmunity());
        // heading draw 0.75 -> 3pi/2, i.e. straight down, out of the area at the bottom side
        ScriptedRng rng = new ScriptedRng(0.0, 0.05, 0.5, 0.75, 0.9, 0.5);

        Person p = policy.maybeEnter(rng, counter()::getAndIncrement).orElseThrow();

        assertEquals(1.4, p.motion().velocity().getY(), DELTA);
    }

    @Test
    void scriptedInfectedEntrantGetsSymptomsAndTheShortestInfection() {
        EntryPolicy policy = new EntryPolicy(SimulationConfig.noImmunity());
        // ... not immune (0.9), infected (0.05 < 0.1), symptomatic (0.3 < 0.5), duration 500 (0.0)
        ScriptedRng rng = new ScriptedRng(0.0, 0.05, 0.5, 0.25, 0.9, 0.05, 0.3, 0.0);

        Person p = policy.maybeEnter(rng, counter()::getAndIncrement).orElseThrow();

        assertEquals(HealthKind.INFECTED_SYMPTOMATIC, p.kind());
        assertEquals(500, p.infectionTicksLeft());
        assertEquals(8, rng.state());
    }

    @Test
    void infectionDrawAtExactlyTenPercentMeansNotInfected() {
        EntryPolicy policy = new EntryPolicy(SimulationConfig.noImmunity());

        Person p = policy.maybeEnter(new ScriptedRng(0.0, 0.05, 0.5, 0.25, 0.9, 0.10),
                counter()::getAndIncrement).orElseThrow();

        assertEquals(HealthKind.HEALTHY, p.kind());
    }

    @Test
    void scriptedImmuneEntrantIsNeverDrawnForInfection() {
        EntryPolicy policy = new EntryPolicy(SimulationConfig.withImmunity(0.3));
        ScriptedRng rng = new ScriptedRng(0.0, 0.05, 0.5, 0.25, 0.29);

        Person p = policy.maybeEnter(rng, counter()::getAndIncrement).orElseThrow();

        assertEquals(HealthKind.IMMUNE, p.kind());
        assertEquals(5, rng.state());
    }

    @Test
    void entrantsAlwaysStartOnTheBorderMovingInside() {
        SimulationConfig config = config(0, 0, 25.0, 0.0);   // one entrant per tick
        Area area = new Area(30, 30);
        EntryPolicy policy = new EntryPolicy(config);
        Rng rng = new SplitMix64Rng(6);

        for (int i = 0; i < 20_000; i++) {
            Person p = policy.maybeEnter(rng, counter()::getAndIncrement).orElseThrow();
            Vector2D position = p.motion().position();

            assertTrue(onBorder(position, area));
            assertTrue(area.contains(position.plus(p.motion().velocity().times(1e-6))), "moves outside");
            assertTrue(p.motion().speed() >= config.minSpeed() - DELTA
                    && p.motion().speed() <= SimulationConfig.MAX_SPEED + DELTA);
        }
    }

    @Test
    void aboutTenPercentOfEntrantsAreInfectedWhenNobodyIsImmune() {
        EntryPolicy policy = new EntryPolicy(config(0, 0, 25.0, 0.0));
        Rng rng = new SplitMix64Rng(7);
        int n = 100_000;
        int infected = 0;
        int immune = 0;

        for (int i = 0; i < n; i++) {
            HealthKind kind = policy.maybeEnter(rng, counter()::getAndIncrement).orElseThrow().kind();
            infected += kind.isInfected() ? 1 : 0;
            immune += kind == HealthKind.IMMUNE ? 1 : 0;
        }

        assertEquals(0.10, (double) infected / n, 0.01);
        assertEquals(0, immune);
    }

    @Test
    void withImmunityTheShareOfImmuneAndInfectedEntrantsFollowsTheDrawOrder() {
        EntryPolicy policy = new EntryPolicy(config(0, 0, 25.0, 0.3));
        Rng rng = new SplitMix64Rng(8);
        int n = 100_000;
        int infected = 0;
        int immune = 0;

        for (int i = 0; i < n; i++) {
            HealthKind kind = policy.maybeEnter(rng, counter()::getAndIncrement).orElseThrow().kind();
            infected += kind.isInfected() ? 1 : 0;
            immune += kind == HealthKind.IMMUNE ? 1 : 0;
        }

        assertEquals(0.30, (double) immune / n, 0.01);
        assertEquals(0.07, (double) infected / n, 0.01);   // 70% susceptible * 10%
    }

    @Test
    void entriesHappenWithTheConfiguredFrequency() {
        EntryPolicy policy = new EntryPolicy(SimulationConfig.noImmunity());
        Rng rng = new SplitMix64Rng(10);
        int ticks = 200_000;
        int entries = 0;

        for (int i = 0; i < ticks; i++) {
            if (policy.maybeEnter(rng, counter()::getAndIncrement).isPresent()) {
                entries++;
            }
        }

        assertEquals(0.072, (double) entries / ticks, 0.003);
    }

    @Test
    void sameSeedGivesTheSameInitialPopulation() {
        EntryPolicy policy = new EntryPolicy(SimulationConfig.withImmunity(0.3));

        List<Person> a = policy.createInitialPopulation(new SplitMix64Rng(77), counter()::getAndIncrement);
        List<Person> b = policy.createInitialPopulation(new SplitMix64Rng(77), counter()::getAndIncrement);

        for (int i = 0; i < a.size(); i++) {
            assertArrayEquals(a.get(i).motion().position().getComponents(), b.get(i).motion().position().getComponents());
            assertArrayEquals(a.get(i).motion().velocity().getComponents(), b.get(i).motion().velocity().getComponents());
            assertEquals(a.get(i).kind(), b.get(i).kind());
        }
        assertFalse(a.isEmpty());
    }
}
