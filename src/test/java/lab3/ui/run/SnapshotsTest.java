package lab3.ui.run;

import lab3.core.Simulation;
import lab3.core.SimulationConfig;
import lab3.core.SimulationEngine;
import lab3.core.memento.SimulationMemento;
import lab3.core.view.WorldView;
import lab3.ui.run.Snapshots.Item;
import lab3.ui.run.Snapshots.Kind;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnapshotsTest {
    private static Simulation simulation(long seed) {
        return Simulation.withSeed(SimulationConfig.noImmunity(), seed);
    }

    /** Runs the way the window does: one step, then {@code afterStep()}, again and again. */
    private static void run(SimulationEngine engine, Snapshots snapshots, int steps) {
        for (int i = 0; i < steps; i++) {
            engine.step();
            snapshots.afterStep();
        }
    }

    private static List<Long> ticks(List<Item> items, Kind kind) {
        return items.stream().filter(i -> i.kind() == kind).map(Item::tick).toList();
    }

    // ---- mementos of the user

    @Test
    void startsEmpty() {
        Snapshots snapshots = new Snapshots(simulation(1));

        assertEquals(List.of(), snapshots.items());
        assertFalse(snapshots.autoEnabled());
        assertEquals(0, snapshots.version());
    }

    @Test
    void takingMementosNumbersThemAndRecordsTheMoment() {
        Simulation sim = simulation(1);
        Snapshots snapshots = new Snapshots(sim);

        Item first = snapshots.takeManual();
        run(sim, snapshots, 50);
        Item second = snapshots.takeManual();

        assertEquals(new Item(Kind.MANUAL, 1, 0, 0.0), first);
        assertEquals(new Item(Kind.MANUAL, 2, 50, 2.0), second);
        assertEquals(List.of(first, second), snapshots.items());
    }

    @Test
    void restoringBringsBackTheMomentAndTheRunContinuesIdentically() {
        Simulation sim = simulation(7);
        Snapshots snapshots = new Snapshots(sim);
        run(sim, snapshots, 120);
        Item item = snapshots.takeManual();
        WorldView atMemento = sim.view();
        run(sim, snapshots, 200);
        WorldView straightOn = sim.view();

        snapshots.restore(item);

        assertEquals(atMemento, sim.view());
        run(sim, snapshots, 200);
        assertEquals(straightOn, sim.view());
    }

    @Test
    void aMementoCanBeRestoredAgainAndAgain() {
        Simulation sim = simulation(3);
        Snapshots snapshots = new Snapshots(sim);
        run(sim, snapshots, 30);
        Item item = snapshots.takeManual();
        WorldView atMemento = sim.view();

        for (int i = 0; i < 3; i++) {
            run(sim, snapshots, 40);
            snapshots.restore(item);
            assertEquals(atMemento, sim.view());
        }
        assertEquals(List.of(item), snapshots.items());
    }

    @Test
    void aMementoFromAnotherRunIsRestoredWhateverTheSimulationDoesNow() {
        Simulation other = simulation(99);
        run(other, new Snapshots(other), 80);
        SimulationMemento fromOtherRun = other.createMemento();
        WorldView otherView = other.view();

        Simulation sim = simulation(5);
        Snapshots snapshots = new Snapshots(sim);
        Item loaded = snapshots.addManual(fromOtherRun);

        assertEquals(new Item(Kind.MANUAL, 1, 80, 3.2), loaded);
        snapshots.restore(loaded);
        assertEquals(otherView, sim.view());
    }

    @Test
    void theMementoCanBeTakenOutForSaving() {
        Simulation sim = simulation(2);
        Snapshots snapshots = new Snapshots(sim);
        run(sim, snapshots, 60);
        SimulationMemento expected = sim.createMemento();

        Item item = snapshots.takeManual();
        run(sim, snapshots, 60);

        assertEquals(expected, snapshots.memento(item));
    }

    @Test
    void anAutomaticMementoCanBeTakenOutForSavingToo() {
        Simulation sim = simulation(2);
        Snapshots snapshots = new Snapshots(sim);
        snapshots.setAutoEnabled(true);
        run(sim, snapshots, 25);
        SimulationMemento atSecondOne = sim.createMemento();
        run(sim, snapshots, 50);
        Item item = snapshots.items().stream().filter(i -> i.tick() == 25).findFirst().orElseThrow();

        assertEquals(atSecondOne, snapshots.memento(item));
    }

    @Test
    void theOldestMementoOfTheUserIsDroppedWhenTheListIsFull() {
        Simulation sim = simulation(1);
        Snapshots snapshots = new Snapshots(sim, 2, 5);
        Item first = snapshots.takeManual();
        Item second = snapshots.takeManual();
        Item third = snapshots.takeManual();

        assertEquals(List.of(second, third), snapshots.items());
        assertThrows(IllegalArgumentException.class, () -> snapshots.restore(first));
        assertThrows(IllegalArgumentException.class, () -> snapshots.memento(first));
    }

    // ---- automatic mementos

    @Test
    void automaticMementosAreOffUntilSwitchedOn() {
        Simulation sim = simulation(1);
        Snapshots snapshots = new Snapshots(sim);

        run(sim, snapshots, 100);

        assertEquals(List.of(), snapshots.items());
    }

    @Test
    void switchedOnAtTheStartItTakesOneAtTimeZeroAndThenOneEverySecond() {
        Simulation sim = simulation(1);
        Snapshots snapshots = new Snapshots(sim);

        snapshots.setAutoEnabled(true);
        run(sim, snapshots, 100);

        assertEquals(List.of(0L, 25L, 50L, 75L, 100L), ticks(snapshots.items(), Kind.AUTO));
        assertEquals(List.of(0.0, 1.0, 2.0, 3.0, 4.0),
                snapshots.items().stream().map(Item::seconds).toList());
    }

    @Test
    void switchedOnInTheMiddleOfASecondItWaitsForTheNextWholeSecond() {
        Simulation sim = simulation(1);
        Snapshots snapshots = new Snapshots(sim);
        run(sim, snapshots, 10);

        snapshots.setAutoEnabled(true);
        assertEquals(List.of(), snapshots.items());

        run(sim, snapshots, 15);
        assertEquals(List.of(25L), ticks(snapshots.items(), Kind.AUTO));
    }

    @Test
    void switchedOnExactlyAtAWholeSecondItTakesOneAtOnce() {
        Simulation sim = simulation(1);
        Snapshots snapshots = new Snapshots(sim);
        run(sim, snapshots, 50);

        snapshots.setAutoEnabled(true);

        assertEquals(List.of(50L), ticks(snapshots.items(), Kind.AUTO));
    }

    @Test
    void switchingOffStopsThemAndSwitchingOnAgainAddsNoDuplicate() {
        Simulation sim = simulation(1);
        Snapshots snapshots = new Snapshots(sim);
        snapshots.setAutoEnabled(true);
        run(sim, snapshots, 50);

        snapshots.setAutoEnabled(false);
        run(sim, snapshots, 50);
        assertEquals(List.of(0L, 25L, 50L), ticks(snapshots.items(), Kind.AUTO));

        snapshots.setAutoEnabled(true);          // now at tick 100: due at once
        snapshots.setAutoEnabled(true);          // no change
        assertEquals(List.of(0L, 25L, 50L, 100L), ticks(snapshots.items(), Kind.AUTO));
    }

    @Test
    void eachWholeSecondIsTakenEvenWhenManyStepsAreRunBetweenRedraws() {
        Simulation sim = simulation(1);
        Snapshots snapshots = new Snapshots(sim);
        snapshots.setAutoEnabled(true);

        // the window runs several steps per frame, and calls afterStep() after each of them
        for (int frame = 0; frame < 4; frame++) {
            run(sim, snapshots, 60);
        }

        assertEquals(List.of(0L, 25L, 50L, 75L, 100L, 125L, 150L, 175L, 200L, 225L), ticks(snapshots.items(), Kind.AUTO));
    }

    @Test
    void theOldestAutomaticMementoIsDroppedWhenTheListIsFull() {
        Simulation sim = simulation(1);
        Snapshots snapshots = new Snapshots(sim, 5, 3);
        snapshots.setAutoEnabled(true);

        run(sim, snapshots, 125);

        assertEquals(List.of(75L, 100L, 125L), ticks(snapshots.items(), Kind.AUTO));
    }

    @Test
    void automaticMementosNeverPushOutThoseOfTheUser() {
        Simulation sim = simulation(1);
        Snapshots snapshots = new Snapshots(sim, 2, 2);
        Item mine = snapshots.takeManual();
        snapshots.setAutoEnabled(true);

        run(sim, snapshots, 200);

        assertEquals(List.of(mine), snapshots.items().stream().filter(i -> i.kind() == Kind.MANUAL).toList());
        assertEquals(2, ticks(snapshots.items(), Kind.AUTO).size());
    }

    @Test
    void theListShowsTheOnesOfTheUserFirst() {
        Simulation sim = simulation(1);
        Snapshots snapshots = new Snapshots(sim);
        snapshots.setAutoEnabled(true);
        run(sim, snapshots, 30);
        snapshots.takeManual();

        List<Kind> kinds = snapshots.items().stream().map(Item::kind).toList();

        assertEquals(List.of(Kind.MANUAL, Kind.AUTO, Kind.AUTO), kinds);
    }

    @Test
    void restoringAnAutomaticMementoKeepsTheLaterOnesAndAddsNoDuplicates() {
        Simulation sim = simulation(4);
        Snapshots snapshots = new Snapshots(sim);
        snapshots.setAutoEnabled(true);
        run(sim, snapshots, 100);
        Item item50 = snapshots.items().stream().filter(i -> i.tick() == 50).findFirst().orElseThrow();
        WorldView straightOn = sim.view();

        snapshots.restore(item50);

        assertEquals(50, sim.view().tick());
        assertEquals(List.of(0L, 25L, 50L, 75L, 100L), ticks(snapshots.items(), Kind.AUTO));

        run(sim, snapshots, 50);                               // back at tick 100, along the same way
        assertEquals(straightOn, sim.view());
        assertEquals(List.of(0L, 25L, 50L, 75L, 100L), ticks(snapshots.items(), Kind.AUTO));
    }

    @Test
    void restoringAMementoOfTheUserEmptiesTheAutomaticList() {
        Simulation sim = simulation(4);
        Snapshots snapshots = new Snapshots(sim);
        snapshots.setAutoEnabled(true);
        run(sim, snapshots, 60);
        Item mine = snapshots.takeManual();
        run(sim, snapshots, 60);
        assertEquals(List.of(0L, 25L, 50L, 75L, 100L), ticks(snapshots.items(), Kind.AUTO));

        snapshots.restore(mine);                                // back at tick 60

        assertEquals(List.of(), ticks(snapshots.items(), Kind.AUTO));
        run(sim, snapshots, 30);                                // they start again, now from this moment
        assertEquals(List.of(75L), ticks(snapshots.items(), Kind.AUTO));
        assertEquals(List.of(mine), snapshots.items().stream().filter(i -> i.kind() == Kind.MANUAL).toList());
    }

    @Test
    void restoringAnAutomaticMementoGivesTheExactMomentItWasTakenAt() {
        Simulation sim = simulation(11);
        Snapshots snapshots = new Snapshots(sim);
        snapshots.setAutoEnabled(true);
        run(sim, snapshots, 25);
        WorldView at25 = sim.view();
        run(sim, snapshots, 100);

        Item item = snapshots.items().stream().filter(i -> i.tick() == 25).findFirst().orElseThrow();
        snapshots.restore(item);

        assertEquals(at25, sim.view());
    }

    // ---- version

    @Test
    void theVersionGrowsOnlyWhenTheListChanges() {
        Simulation sim = simulation(1);
        Snapshots snapshots = new Snapshots(sim);
        int v0 = snapshots.version();

        run(sim, snapshots, 30);                                // auto off: nothing changes
        assertEquals(v0, snapshots.version());

        Item item = snapshots.takeManual();
        int v1 = snapshots.version();
        assertTrue(v1 > v0);

        snapshots.setAutoEnabled(true);                         // tick 30: not a whole second
        assertEquals(v1, snapshots.version());

        run(sim, snapshots, 10);                                // tick 40: not a whole second
        assertEquals(v1, snapshots.version());
        run(sim, snapshots, 10);                                // tick 50: a whole second
        int v2 = snapshots.version();
        assertTrue(v2 > v1);

        snapshots.restore(item);
        assertTrue(snapshots.version() > v2);
    }

    // ---- bad use

    @Test
    void nothingIsChangedWhenAMementoIsNotKeptAnyMore() {
        Simulation sim = simulation(1);
        Snapshots snapshots = new Snapshots(sim, 1, 5);
        Item old = snapshots.takeManual();
        snapshots.takeManual();                                  // drops the first
        run(sim, snapshots, 10);
        WorldView before = sim.view();
        int version = snapshots.version();

        assertThrows(IllegalArgumentException.class, () -> snapshots.restore(old));

        assertEquals(before, sim.view());
        assertEquals(version, snapshots.version());
    }

    @Test
    void aFailedRestoreOfTheUserDoesNotEmptyTheAutomaticList() {
        Simulation sim = simulation(1);
        Snapshots snapshots = new Snapshots(sim, 1, 5);
        Item old = snapshots.takeManual();
        snapshots.takeManual();
        snapshots.setAutoEnabled(true);
        run(sim, snapshots, 50);

        assertThrows(IllegalArgumentException.class, () -> snapshots.restore(old));

        assertEquals(List.of(0L, 25L, 50L), ticks(snapshots.items(), Kind.AUTO));
    }

    @Test
    void badArgumentsAreRejected() {
        Simulation sim = simulation(1);
        Snapshots snapshots = new Snapshots(sim);

        assertThrows(IllegalArgumentException.class, () -> snapshots.restore(null));
        assertThrows(IllegalArgumentException.class, () -> snapshots.memento(null));
        assertThrows(IllegalArgumentException.class, () -> snapshots.addManual(null));
        assertThrows(IllegalArgumentException.class, () -> new Item(null, 1, 0, 0.0));
        assertThrows(IllegalArgumentException.class, () -> new Snapshots(null));
        assertThrows(IllegalArgumentException.class, () -> new Snapshots(sim, 0, 5));
        assertThrows(IllegalArgumentException.class, () -> new Snapshots(sim, 5, 0));
    }

    @Test
    void kindsHaveTheirPolishTitles() {
        assertEquals("Ręczna", Kind.MANUAL.title());
        assertEquals("Auto", Kind.AUTO.title());
        assertNotEquals(Kind.MANUAL.title(), Kind.AUTO.title());
    }
}
