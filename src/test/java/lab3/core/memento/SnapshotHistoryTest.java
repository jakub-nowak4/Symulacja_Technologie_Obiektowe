package lab3.core.memento;

import lab3.core.Simulation;
import lab3.core.SimulationConfig;
import lab3.core.SimulationEngine;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnapshotHistoryTest {

    private static Simulation simulation(long seed) {
        return Simulation.withSeed(SimulationConfig.noImmunity(), seed);
    }

    private static void run(SimulationEngine engine, int steps) {
        for (int i = 0; i < steps; i++) {
            engine.step();
        }
    }

    private static List<Integer> numbers(SnapshotHistory history) {
        return history.list().stream().map(SnapshotHistory.Entry::number).toList();
    }

    // ---- numbering and listing

    @Test
    void newHistoryIsEmpty() {
        SnapshotHistory history = new SnapshotHistory(simulation(1), 10);

        assertEquals(0, history.size());
        assertEquals(10, history.capacity());
        assertTrue(history.list().isEmpty());
    }

    @Test
    void createNumbersMementosFromOne() {
        Simulation simulation = simulation(1);
        SnapshotHistory history = new SnapshotHistory(simulation, 10);

        assertEquals(1, history.create());
        run(simulation, 10);
        assertEquals(2, history.create());
        assertEquals(3, history.create());
    }

    @Test
    void listShowsNumberTickAndTimeFromTheOldestToTheNewest() {
        Simulation simulation = simulation(2);
        SnapshotHistory history = new SnapshotHistory(simulation, 10);

        history.create();
        run(simulation, 25);
        history.create();
        run(simulation, 50);
        history.create();

        assertEquals(List.of(
                new SnapshotHistory.Entry(1, 0, 0.0),
                new SnapshotHistory.Entry(2, 25, 1.0),
                new SnapshotHistory.Entry(3, 75, 3.0)), history.list());
    }

    @Test
    void listIsACopy() {
        SnapshotHistory history = new SnapshotHistory(simulation(3), 10);
        history.create();
        List<SnapshotHistory.Entry> before = history.list();

        history.create();

        assertEquals(1, before.size());
        assertEquals(2, history.list().size());
    }

    // ---- restoring

    @Test
    void restoreSendsTheSimulationBackAndItContinuesAsBefore() {
        Simulation simulation = simulation(42);
        SnapshotHistory history = new SnapshotHistory(simulation, 10);
        run(simulation, 200);
        int number = history.create();
        run(simulation, 300);
        var finalView = simulation.view();

        history.restore(number);
        assertEquals(200, simulation.view().tick());
        run(simulation, 300);

        assertEquals(finalView, simulation.view());
    }

    @Test
    void anyOfSeveralMementosCanBeRestoredAndRestoringTwiceWorks() {
        Simulation simulation = simulation(5);
        SnapshotHistory history = new SnapshotHistory(simulation, 10);
        int first = history.create();
        run(simulation, 100);
        int second = history.create();
        run(simulation, 100);

        history.restore(second);
        assertEquals(100, simulation.view().tick());
        history.restore(first);
        assertEquals(0, simulation.view().tick());
        history.restore(second);
        assertEquals(100, simulation.view().tick());
        assertEquals(List.of(1, 2), numbers(history));
    }

    @Test
    void mementosTakenAfterGoingBackContinueTheNumbering() {
        Simulation simulation = simulation(6);
        SnapshotHistory history = new SnapshotHistory(simulation, 10);
        int first = history.create();
        run(simulation, 100);
        history.create();

        history.restore(first);
        run(simulation, 30);

        assertEquals(3, history.create());
        assertEquals(List.of(0L, 100L, 30L), history.list().stream().map(SnapshotHistory.Entry::tick).toList());
    }

    @Test
    void restoreRejectsUnknownNumbers() {
        SnapshotHistory history = new SnapshotHistory(simulation(7), 10);
        history.create();

        assertThrows(IllegalArgumentException.class, () -> history.restore(0));
        assertThrows(IllegalArgumentException.class, () -> history.restore(2));
        assertThrows(IllegalArgumentException.class, () -> history.restore(-1));
    }

    @Test
    void failedRestoreLeavesTheSimulationAlone() {
        Simulation simulation = simulation(8);
        SnapshotHistory history = new SnapshotHistory(simulation, 10);
        run(simulation, 50);
        var before = simulation.view();

        assertThrows(IllegalArgumentException.class, () -> history.restore(1));

        assertEquals(before, simulation.view());
    }

    // ---- capacity

    @Test
    void oldestMementoIsDroppedWhenTheCapacityIsReached() {
        Simulation simulation = simulation(9);
        SnapshotHistory history = new SnapshotHistory(simulation, 3);

        for (int i = 0; i < 5; i++) {
            history.create();
            run(simulation, 10);
        }

        assertEquals(3, history.size());
        assertEquals(List.of(3, 4, 5), numbers(history));
        assertEquals(List.of(20L, 30L, 40L), history.list().stream().map(SnapshotHistory.Entry::tick).toList());
    }

    @Test
    void droppedNumbersAreNotRestorableAndNotUsedAgain() {
        Simulation simulation = simulation(10);
        SnapshotHistory history = new SnapshotHistory(simulation, 2);
        history.create();
        history.create();
        history.create();

        assertThrows(IllegalArgumentException.class, () -> history.restore(1));
        history.restore(2);
        history.restore(3);
        assertEquals(4, history.create());
    }

    @Test
    void capacityOfOneKeepsOnlyTheLatest() {
        Simulation simulation = simulation(11);
        SnapshotHistory history = new SnapshotHistory(simulation, 1);
        history.create();
        run(simulation, 25);
        history.create();

        assertEquals(List.of(2), numbers(history));
        history.restore(2);
        assertEquals(25, simulation.view().tick());
    }

    @Test
    void automaticMementoEverySecondForTenMinutesFitsInTheLimit() {
        Simulation simulation = simulation(12);
        SnapshotHistory history = new SnapshotHistory(simulation, 60);

        history.create();
        for (int second = 1; second <= 120; second++) {
            run(simulation, 25);
            history.create();
        }

        assertEquals(60, history.size());
        assertEquals(121, history.list().get(59).number());
        assertEquals(120 * 25L, history.list().get(59).tick());
        assertEquals(61 * 25L, history.list().get(0).tick());
    }

    // ---- foreign mementos, get, arguments

    @Test
    void getReturnsTheKeptMementoItself() {
        Simulation simulation = simulation(13);
        SnapshotHistory history = new SnapshotHistory(simulation, 10);
        int number = history.create();

        assertSame(history.get(number), history.get(number));
        assertEquals(simulation.createMemento(), history.get(number));
    }

    @Test
    void addKeepsAMementoFromElsewhereAndItCanBeRestored() {
        Simulation other = simulation(14);
        run(other, 400);
        SimulationMemento foreign = other.createMemento();
        Simulation simulation = simulation(15);
        SnapshotHistory history = new SnapshotHistory(simulation, 10);
        history.create();

        int number = history.add(foreign);
        assertEquals(2, number);
        history.restore(number);

        assertEquals(400, simulation.view().tick());
        assertEquals(other.view(), simulation.view());
    }

    @Test
    void addDropsTheOldestWhenFullToo() {
        Simulation simulation = simulation(16);
        SnapshotHistory history = new SnapshotHistory(simulation, 2);
        history.create();
        history.create();

        history.add(simulation.createMemento());

        assertEquals(List.of(2, 3), numbers(history));
    }

    @Test
    void takingAMementoDoesNotChangeTheSimulation() {
        Simulation withHistory = simulation(17);
        Simulation without = simulation(17);
        SnapshotHistory history = new SnapshotHistory(withHistory, 5);

        for (int i = 0; i < 300; i++) {
            withHistory.step();
            without.step();
            if (i % 10 == 0) {
                history.create();
            }
        }

        assertEquals(without.view(), withHistory.view());
    }

    @Test
    void rejectsWrongArguments() {
        assertThrows(IllegalArgumentException.class, () -> new SnapshotHistory(null, 5));
        assertThrows(IllegalArgumentException.class, () -> new SnapshotHistory(simulation(1), 0));
        assertThrows(IllegalArgumentException.class, () -> new SnapshotHistory(simulation(1), -3));
        assertThrows(IllegalArgumentException.class, () -> new SnapshotHistory(simulation(1), 5).add(null));
    }
}
