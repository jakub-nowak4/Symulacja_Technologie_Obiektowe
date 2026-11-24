package lab3.core;

import lab3.core.health.HealthKind;
import lab3.core.health.ImmuneState;
import lab3.core.health.InfectedSymptomaticState;
import lab3.core.view.PersonView;
import lab3.core.view.WorldView;
import lab3.vectors.Vector2D;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulationTest {

    /** Everybody stays where they are: no velocity changes and no entries. */
    private static SimulationConfig frozen(double symptomatic) {
        return new SimulationConfig(30, 30, 1, 0, 0.0, 0.3, 0.0, symptomatic, 0.0, Math.PI / 4, 0.5);
    }

    private static Person standing(int id, double x, double y) {
        return Person.healthy(id, new Motion(new Vector2D(x, y), new Vector2D(0, 0)));
    }

    private static Person infectedStanding(int id, double x, double y, long ticksLeft) {
        return new Person(id, new Motion(new Vector2D(x, y), new Vector2D(0, 0)),
                InfectedSymptomaticState.INSTANCE, ticksLeft);
    }

    private static Simulation scripted(SimulationConfig config, long seed, Person... people) {
        List<Person> list = new ArrayList<>(List.of(people));
        return new Simulation(config, new SplitMix64Rng(seed), list, 0, list.size());
    }

    private static HealthKind kindOf(WorldView view, int id) {
        for (PersonView p : view.persons()) {
            if (p.id() == id) {
                return p.kind();
            }
        }
        throw new AssertionError("no person with id " + id);
    }

    private static void run(Simulation simulation, int steps) {
        for (int i = 0; i < steps; i++) {
            simulation.step();
        }
    }

    // ---- start, time

    @Test
    void startsAtTickZeroWithTheInitialPopulation() {
        WorldView view = Simulation.withSeed(SimulationConfig.noImmunity(), 1).view();

        assertEquals(0, view.tick());
        assertEquals(30, view.areaWidth(), 0);
        assertEquals(30, view.areaHeight(), 0);
        assertEquals(60, view.persons().size());
        for (int i = 0; i < 60; i++) {
            assertEquals(i, view.persons().get(i).id());
        }
    }

    @Test
    void everyStepIsOneTickAndTwentyFiveStepsAreOneSecond() {
        Simulation simulation = Simulation.withSeed(SimulationConfig.noImmunity(), 2);

        simulation.step();
        assertEquals(1, simulation.view().tick());

        run(simulation, 24);
        assertEquals(25, simulation.view().tick());
        assertEquals(1.0, simulation.view().seconds(), 1e-12);
    }

    @Test
    void viewIsASnapshotThatLaterStepsDoNotChange() {
        Simulation simulation = Simulation.withSeed(SimulationConfig.noImmunity(), 3);
        WorldView before = simulation.view();
        List<PersonView> copy = new ArrayList<>(before.persons());

        run(simulation, 50);

        assertEquals(0, before.tick());
        assertEquals(copy, before.persons());
    }

    // ---- determinism

    @Test
    void sameSeedGivesTheSameRunStepByStep() {
        Simulation a = Simulation.withSeed(SimulationConfig.noImmunity(), 42);
        Simulation b = Simulation.withSeed(SimulationConfig.noImmunity(), 42);

        for (int i = 0; i < 1500; i++) {
            a.step();
            b.step();
            assertEquals(a.view(), b.view(), "tick " + (i + 1));
        }
    }

    @Test
    void differentSeedsGiveDifferentRuns() {
        Simulation a = Simulation.withSeed(SimulationConfig.noImmunity(), 1);
        Simulation b = Simulation.withSeed(SimulationConfig.noImmunity(), 2);

        run(a, 100);
        run(b, 100);

        assertNotEquals(a.view(), b.view());
    }

    // ---- population and area

    @Test
    void peopleStayInsideTheAreaAndIdsStayUniqueAndAscending() {
        SimulationConfig config = SimulationConfig.noImmunity();
        Simulation simulation = Simulation.withSeed(config, 5);
        Area area = new Area(config.areaWidth(), config.areaHeight());

        for (int i = 0; i < 1500; i++) {
            simulation.step();
            int previousId = -1;
            for (PersonView p : simulation.view().persons()) {
                assertTrue(area.contains(new Vector2D(p.x(), p.y())), "tick " + (i + 1));
                assertTrue(p.id() > previousId, "ids must ascend, tick " + (i + 1));
                previousId = p.id();
            }
        }
    }

    @Test
    void withoutEntriesNobodyNewAppears() {
        SimulationConfig config = new SimulationConfig(10, 10, 40, 0, 0.0, 0.5, 0.0, 0.5, 0.02, Math.PI / 4, 0.5);
        Simulation simulation = Simulation.withSeed(config, 6);
        int previous = simulation.view().persons().size();

        for (int i = 0; i < 1500; i++) {
            simulation.step();
            int now = simulation.view().persons().size();
            assertTrue(now <= previous, "population grew without entries at tick " + (i + 1));
            previous = now;
        }
        assertTrue(previous < 40, "in a small area some people should have left");
    }

    @Test
    void entriesAddPeopleWithNewIdsOnly() {
        SimulationConfig config = new SimulationConfig(30, 30, 20, 0, 3.0, 0.3, 0.0, 0.5, 0.02, Math.PI / 4, 0.5);
        Simulation simulation = Simulation.withSeed(config, 7);
        Set<Integer> seen = new HashSet<>();
        int maxId = 19;
        for (PersonView p : simulation.view().persons()) {
            seen.add(p.id());
        }

        for (int i = 0; i < 1000; i++) {
            simulation.step();
            for (PersonView p : simulation.view().persons()) {
                if (seen.add(p.id())) {
                    assertEquals(maxId + 1, p.id(), "new ids must be consecutive");
                    maxId = p.id();
                }
            }
        }
        assertTrue(maxId > 19, "somebody should have entered");
    }

    @Test
    void populationStaysAroundTheInitialSizeWithTheCalibratedEntryRate() {
        // entry rate from docs/PLAN.md section 5 for N = 60 on 30 x 30 m; final value is chosen in stage 17
        Simulation simulation = Simulation.withSeed(SimulationConfig.noImmunity(), 8);
        double sum = 0;
        int steps = 5000;
        for (int i = 0; i < steps; i++) {
            simulation.step();
            sum += simulation.view().persons().size();
        }
        double mean = sum / steps;

        assertTrue(mean > 30 && mean < 120, "mean population " + mean);
    }

    // ---- infection

    @Test
    void healthyPersonNextToAnInfectedOneCatchesItAfterExactlySeventyFiveTicks() {
        Simulation simulation = scripted(frozen(1.0),
                1, infectedStanding(0, 10, 10, 2000), standing(1, 11, 10));

        run(simulation, 74);
        assertEquals(HealthKind.HEALTHY, kindOf(simulation.view(), 1));

        simulation.step();
        assertEquals(75, simulation.view().tick());
        assertEquals(HealthKind.INFECTED_SYMPTOMATIC, kindOf(simulation.view(), 1));
    }

    @Test
    void personTwoAndAHalfMetresAwayIsNeverInfected() {
        Simulation simulation = scripted(frozen(1.0),
                1, infectedStanding(0, 10, 10, 2000), standing(1, 12.5, 10));

        run(simulation, 500);

        assertEquals(HealthKind.HEALTHY, kindOf(simulation.view(), 1));
    }

    @Test
    void newlyInfectedPersonSpreadsOnlyFromTheNextTick() {
        // 0 -- 1 -- 2 in a row, 1.9 m apart: 2 is out of reach of 0 and can only be infected by 1
        Simulation simulation = scripted(frozen(1.0),
                1, infectedStanding(0, 10, 10, 2000), standing(1, 11.9, 10), standing(2, 13.8, 10));

        run(simulation, 149);
        assertEquals(HealthKind.INFECTED_SYMPTOMATIC, kindOf(simulation.view(), 1));
        assertEquals(HealthKind.HEALTHY, kindOf(simulation.view(), 2));

        simulation.step();
        assertEquals(150, simulation.view().tick());
        assertEquals(HealthKind.INFECTED_SYMPTOMATIC, kindOf(simulation.view(), 2));
    }

    @Test
    void immunePersonIsNeverInfected() {
        Person immune = new Person(1, new Motion(new Vector2D(11, 10), new Vector2D(0, 0)), ImmuneState.INSTANCE);
        List<Person> list = new ArrayList<>(List.of(infectedStanding(0, 10, 10, 5000), immune));
        Simulation simulation = new Simulation(frozen(1.0), new SplitMix64Rng(1), list, 0, 2);

        run(simulation, 500);

        assertEquals(HealthKind.IMMUNE, kindOf(simulation.view(), 1));
    }

    @Test
    void illnessLastsExactlyTheRemainingTicksAndThenTheSourceIsImmune() {
        Simulation simulation = scripted(frozen(1.0), 1, infectedStanding(0, 10, 10, 600));

        run(simulation, 599);
        assertEquals(HealthKind.INFECTED_SYMPTOMATIC, kindOf(simulation.view(), 0));

        simulation.step();
        assertEquals(HealthKind.IMMUNE, kindOf(simulation.view(), 0));
    }

    @Test
    void newInfectionLastsItsFullLengthOfFiveHundredToSevenHundredFiftyTicks() {
        Simulation simulation = scripted(frozen(1.0),
                1, infectedStanding(0, 10, 10, 5000), standing(1, 11, 10));
        run(simulation, 75);
        assertEquals(HealthKind.INFECTED_SYMPTOMATIC, kindOf(simulation.view(), 1));

        long infectedAt = simulation.view().tick();
        while (kindOf(simulation.view(), 1) != HealthKind.IMMUNE) {
            simulation.step();
            assertTrue(simulation.view().tick() < 5000, "should have recovered by now");
        }
        long duration = simulation.view().tick() - infectedAt;

        assertTrue(duration >= 500 && duration <= 750, "duration " + duration);
    }

    @Test
    void infectionSpreadsInTheCrowdedScenario() {
        SimulationConfig config = new SimulationConfig(10, 10, 30, 3, 1.0, 0.3, 0.0, 0.5, 0.02, Math.PI / 4, 0.5);
        Simulation simulation = Simulation.withSeed(config, 9);
        int peak = simulation.view().stats().infected();

        for (int i = 0; i < 1500; i++) {
            simulation.step();
            peak = Math.max(peak, simulation.view().stats().infected());
        }

        assertTrue(peak > 3, "infection should spread beyond the 3 initial cases, peak " + peak);
    }

    @Test
    void peopleRecoverIntoImmunity() {
        SimulationConfig config = new SimulationConfig(10, 10, 30, 3, 1.0, 0.3, 0.0, 0.5, 0.02, Math.PI / 4, 0.5);
        Simulation simulation = Simulation.withSeed(config, 10);

        run(simulation, 1000);

        assertTrue(simulation.view().stats().immune() > 0);
    }

    @Test
    void scenarioWithoutImmunityStartsWithNoInfectedPeople() {
        WorldView start = Simulation.withSeed(SimulationConfig.noImmunity(), 11).view();

        assertEquals(60, start.stats().healthy());
        assertEquals(0, start.stats().infected());
    }

    @Test
    void newInfectionLastsExactlyTheDrawnNumberOfTicks() {
        // every draw is 0.0: nobody changes velocity or enters, the transmission succeeds, the infection
        // shows symptoms and the length is the shortest possible one (500 ticks)
        List<Person> list = new ArrayList<>(List.of(infectedStanding(0, 10, 10, 5000), standing(1, 11, 10)));
        Simulation simulation = new Simulation(frozen(1.0), ScriptedRng.repeatingLast(0.0), list, 0, 2);

        run(simulation, 75);
        assertEquals(HealthKind.INFECTED_SYMPTOMATIC, kindOf(simulation.view(), 1));

        run(simulation, 499);
        assertEquals(574, simulation.view().tick());
        assertEquals(HealthKind.INFECTED_SYMPTOMATIC, kindOf(simulation.view(), 1));

        simulation.step();
        assertEquals(575, simulation.view().tick());
        assertEquals(HealthKind.IMMUNE, kindOf(simulation.view(), 1));
    }

    // ---- construction

    @Test
    void rejectsPersonsThatAreNotSortedById() {
        List<Person> list = new ArrayList<>(List.of(standing(3, 1, 1), standing(2, 2, 2)));

        assertThrows(IllegalArgumentException.class,
                () -> new Simulation(frozen(0.5), new SplitMix64Rng(1), list, 0, 4));
    }

    @Test
    void rejectsNextIdThatCouldClashWithAnExistingPerson() {
        List<Person> list = new ArrayList<>(List.of(standing(0, 1, 1), standing(5, 2, 2)));

        assertThrows(IllegalArgumentException.class,
                () -> new Simulation(frozen(0.5), new SplitMix64Rng(1), list, 0, 5));
    }

    @Test
    void rejectsNullsAndNegativeTick() {
        assertThrows(IllegalArgumentException.class,
                () -> new Simulation(null, new SplitMix64Rng(1), new ArrayList<>(), 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new Simulation(frozen(0.5), new SplitMix64Rng(1), new ArrayList<>(), -1, 0));
    }
}
