package lab3.ui.run;

import lab3.core.Simulation;
import lab3.core.SimulationConfig;
import lab3.core.health.HealthKind;
import lab3.core.view.PersonView;
import lab3.core.view.WorldView;
import lab3.ui.run.StatsHistory.Sample;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatsHistoryTest {
    /** A simulation that runs on and gives a view for every tick it has been at. */
    private static final class Run {
        final Simulation simulation = Simulation.withSeed(SimulationConfig.noImmunity(), 42);
        final Map<Long, WorldView> views = new HashMap<>();

        Run() {
            remember();
        }

        WorldView step() {
            simulation.step();
            return remember();
        }

        WorldView stepTo(long tick) {
            WorldView view = simulation.view();
            while (view.tick() < tick) {
                view = step();
            }
            return view;
        }

        private WorldView remember() {
            WorldView view = simulation.view();
            views.put(view.tick(), view);
            return view;
        }
    }

    private static List<Long> ticks(StatsHistory history) {
        return history.samples().stream().map(Sample::tick).toList();
    }

    @Test
    void startsEmpty() {
        StatsHistory history = new StatsHistory();

        assertEquals(List.of(), history.samples());
        assertEquals(Optional.empty(), history.peakInfected());
        assertEquals(0, history.version());
    }

    @Test
    void theFirstViewIsTheFirstSample() {
        Run run = new Run();
        StatsHistory history = new StatsHistory();

        history.update(run.simulation.view());

        assertEquals(List.of(0L), ticks(history));
        assertEquals(run.simulation.view().stats(), history.samples().get(0).stats());
    }

    @Test
    void oneSamplePerSimulatedSecondWhenEveryTickIsShown() {
        Run run = new Run();
        StatsHistory history = new StatsHistory();
        history.update(run.simulation.view());

        for (int i = 0; i < 100; i++) {
            history.update(run.step());
        }

        assertEquals(List.of(0L, 25L, 50L, 75L, 100L), ticks(history));
    }

    @Test
    void eachSampleHoldsTheCountsOfItsMoment() {
        Run run = new Run();
        StatsHistory history = new StatsHistory();
        history.update(run.simulation.view());
        for (int i = 0; i < 250; i++) {
            history.update(run.step());
        }

        for (Sample sample : history.samples()) {
            assertEquals(run.views.get(sample.tick()).stats(), sample.stats(), "tick " + sample.tick());
            assertEquals(sample.tick() / 25.0, sample.seconds());
        }
    }

    @Test
    void aFrameThatJumpsOverSecondsGivesOneSampleForTheJump() {
        Run run = new Run();
        StatsHistory history = new StatsHistory();
        history.update(run.simulation.view());          // 0

        history.update(run.stepTo(10));                 // too soon
        history.update(run.stepTo(60));                 // 60 - 0 >= 25
        history.update(run.stepTo(61));                 // too soon after 60
        history.update(run.stepTo(90));                 // 90 - 60 >= 25

        assertEquals(List.of(0L, 60L, 90L), ticks(history));
    }

    @Test
    void theSameViewTwiceAddsNothing() {
        Run run = new Run();
        StatsHistory history = new StatsHistory();
        WorldView view = run.simulation.view();

        history.update(view);
        int version = history.version();
        history.update(view);

        assertEquals(List.of(0L), ticks(history));
        assertEquals(version, history.version());
    }

    @Test
    void goingBackDropsTheSamplesOfTheFuture() {
        Run run = new Run();
        StatsHistory history = new StatsHistory();
        history.update(run.simulation.view());
        for (int i = 0; i < 100; i++) {
            history.update(run.step());
        }

        history.update(run.views.get(60L));

        assertEquals(List.of(0L, 25L, 50L), ticks(history));
    }

    @Test
    void goingBackExactlyToASampleKeepsIt() {
        Run run = new Run();
        StatsHistory history = new StatsHistory();
        history.update(run.simulation.view());
        for (int i = 0; i < 100; i++) {
            history.update(run.step());
        }

        history.update(run.views.get(50L));

        assertEquals(List.of(0L, 25L, 50L), ticks(history));
    }

    @Test
    void afterGoingBackTheSamplesContinueFromThere() {
        Run run = new Run();
        StatsHistory history = new StatsHistory();
        history.update(run.simulation.view());
        for (int i = 0; i < 100; i++) {
            history.update(run.step());
        }
        history.update(run.views.get(60L));

        // the same way again: 61 ... 100 (the views of the first run are the same ones)
        for (long tick = 61; tick <= 100; tick++) {
            history.update(run.views.get(tick));
        }

        assertEquals(List.of(0L, 25L, 50L, 75L, 100L), ticks(history));
    }

    @Test
    void goingBackToTimeZeroLeavesOnlyTheFirstSample() {
        Run run = new Run();
        StatsHistory history = new StatsHistory();
        history.update(run.simulation.view());
        for (int i = 0; i < 60; i++) {
            history.update(run.step());
        }

        history.update(run.views.get(0L));

        assertEquals(List.of(0L), ticks(history));
    }

    @Test
    void goingBackBeforeTheOldestSampleStartsAgainFromThatMoment() {
        Run run = new Run();
        StatsHistory history = new StatsHistory(2);
        history.update(run.simulation.view());
        for (int i = 0; i < 100; i++) {
            history.update(run.step());
        }
        assertEquals(List.of(75L, 100L), ticks(history));

        history.update(run.views.get(30L));

        assertEquals(List.of(30L), ticks(history));
    }

    @Test
    void restartForgetsEverythingAndStartsFromTheView() {
        Run run = new Run();
        StatsHistory history = new StatsHistory();
        history.update(run.simulation.view());
        for (int i = 0; i < 100; i++) {
            history.update(run.step());
        }

        int version = history.version();
        history.restart(run.views.get(40L));

        assertNotEquals(version, history.version());
        assertEquals(List.of(40L), ticks(history));
        assertEquals(run.views.get(40L).stats(), history.samples().get(0).stats());
        history.update(run.views.get(60L));          // 20 ticks later: too soon
        history.update(run.views.get(65L));          // 25 ticks later
        assertEquals(List.of(40L, 65L), ticks(history));
    }

    @Test
    void theOldestSamplesAreDroppedWhenTheHistoryIsFull() {
        Run run = new Run();
        StatsHistory history = new StatsHistory(3);
        history.update(run.simulation.view());

        for (int i = 0; i < 125; i++) {
            history.update(run.step());
        }

        assertEquals(List.of(75L, 100L, 125L), ticks(history));
    }

    @Test
    void theCapacityOfTheRealHistoryIsFifteenMinutes() {
        assertEquals(900, StatsHistory.MAX_SAMPLES);
        assertEquals(25, StatsHistory.SAMPLE_TICKS);
    }

    @Test
    void thePeakIsTheSampleWithMostInfectedAndTheEarliestOneWins() {
        Run run = new Run();
        StatsHistory history = new StatsHistory();
        history.update(run.simulation.view());
        for (int i = 0; i < 2500; i++) {
            history.update(run.step());
        }

        Sample peak = history.peakInfected().orElseThrow();

        int most = history.samples().stream().mapToInt(s -> s.stats().infected()).max().orElseThrow();
        assertTrue(most > 0, "the run should have infections");
        assertEquals(most, peak.stats().infected());
        for (Sample sample : history.samples()) {
            if (sample.stats().infected() == most) {
                assertEquals(sample, peak);       // the first one with the most
                break;
            }
        }
    }

    @Test
    void thePeakCountsAllInfectedPeopleNotOnlyThoseWithSymptoms() {
        StatsHistory history = new StatsHistory();
        history.update(world(0, HealthKind.INFECTED_SYMPTOMATIC, HealthKind.HEALTHY));                 // 1 infected
        history.update(world(25, HealthKind.INFECTED_ASYMPTOMATIC, HealthKind.INFECTED_ASYMPTOMATIC)); // 2 infected
        history.update(world(50, HealthKind.INFECTED_SYMPTOMATIC, HealthKind.IMMUNE));                 // 1 infected

        assertEquals(25L, history.peakInfected().orElseThrow().tick());
    }

    private static WorldView world(long tick, HealthKind... kinds) {
        List<PersonView> persons = new ArrayList<>();
        for (int i = 0; i < kinds.length; i++) {
            persons.add(new PersonView(i, 1.0 + i, 1.0, kinds[i]));
        }
        return new WorldView(tick, 30, 30, persons);
    }

    @Test
    void thePeakOfOneSampleIsThatSample() {
        Run run = new Run();
        StatsHistory history = new StatsHistory();
        history.update(run.simulation.view());

        assertEquals(history.samples().get(0), history.peakInfected().orElseThrow());
    }

    @Test
    void theVersionGrowsOnlyWhenTheSamplesChange() {
        Run run = new Run();
        StatsHistory history = new StatsHistory();

        history.update(run.simulation.view());
        int v1 = history.version();
        assertTrue(v1 > 0);

        history.update(run.step());                     // tick 1: nothing new
        assertEquals(v1, history.version());

        history.update(run.stepTo(25));                 // a new sample
        int v2 = history.version();
        assertNotEquals(v1, v2);

        history.update(run.views.get(10L));             // going back drops a sample
        assertNotEquals(v2, history.version());
    }

    @Test
    void theListOfSamplesIsACopy() {
        Run run = new Run();
        StatsHistory history = new StatsHistory();
        history.update(run.simulation.view());
        List<Sample> before = history.samples();

        history.update(run.stepTo(30));

        assertEquals(1, before.size());
        assertEquals(2, history.samples().size());
        assertThrows(UnsupportedOperationException.class, () -> before.add(before.get(0)));
    }

    @Test
    void badArgumentsAreRejected() {
        StatsHistory history = new StatsHistory();

        assertThrows(IllegalArgumentException.class, () -> history.update(null));
        assertThrows(IllegalArgumentException.class, () -> history.restart(null));
        assertThrows(IllegalArgumentException.class, () -> new StatsHistory(0));
        assertThrows(IllegalArgumentException.class, () -> new Sample(0, null));
    }
}
