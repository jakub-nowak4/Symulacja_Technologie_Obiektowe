package lab3.core;

import lab3.core.memento.SimulationMemento;
import lab3.core.view.PersonView;
import lab3.core.view.WorldView;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulationMementoTest {

    private static void run(SimulationEngine engine, int steps) {
        for (int i = 0; i < steps; i++) {
            engine.step();
        }
    }

    /** Steps until a moment with infected people and contacts in progress (a memento there says the most). */
    private static void runUntilBusy(Simulation simulation, int minimumTick) {
        while (true) {
            simulation.step();
            if (simulation.view().tick() >= minimumTick) {
                SimulationMemento m = simulation.createMemento();
                if (!m.contacts().isEmpty() && simulation.view().stats().infected() > 0) {
                    return;
                }
            }
            assertTrue(simulation.view().tick() < minimumTick + 5000, "no busy moment found");
        }
    }

    // ---- acceptance test of docs/PLAN.md section 6

    @Test
    void restoredSimulationContinuesExactlyLikeTheOriginal() {
        Simulation a = Simulation.withSeed(SimulationConfig.noImmunity(), 42);
        run(a, 200);
        SimulationMemento m = a.createMemento();
        run(a, 300);

        Simulation b = Simulation.withSeed(SimulationConfig.noImmunity(), 42);
        b.restore(m);
        assertEquals(200, b.view().tick());
        run(b, 300);

        assertEquals(500, b.view().tick());
        assertEquals(a.view(), b.view());
        assertEquals(a.createMemento(), b.createMemento());
    }

    @Test
    void continuationIsIdenticalStepByStepFromABusyMoment() {
        Simulation a = Simulation.withSeed(SimulationConfig.noImmunity(), 7);
        runUntilBusy(a, 1000);
        SimulationMemento m = a.createMemento();
        assertFalse(m.contacts().isEmpty());

        Simulation b = Simulation.fromMemento(m);

        for (int i = 0; i < 1500; i++) {
            a.step();
            b.step();
            assertEquals(a.view(), b.view(), "step " + (i + 1) + " after the memento");
        }
        assertEquals(a.createMemento(), b.createMemento());
    }

    @Test
    void anyMomentOfARunCanBeRestored() {
        for (long seed : new long[] {1, 2, 3}) {
            Simulation original = Simulation.withSeed(SimulationConfig.noImmunity(), seed);
            List<SimulationMemento> mementos = new java.util.ArrayList<>();
            for (int i = 0; i < 12; i++) {
                run(original, 233);   // odd step so that mementos fall in different phases of contacts
                mementos.add(original.createMemento());
            }

            for (SimulationMemento m : mementos) {
                Simulation copy = Simulation.fromMemento(m);
                long remaining = original.view().tick() - m.tick();
                run(copy, (int) remaining);

                assertEquals(original.createMemento(), copy.createMemento(), "seed " + seed + ", memento at " + m.tick());
            }
        }
    }

    @Test
    void scenarioWithImmunityIsRestoredToo() {
        Simulation a = Simulation.withSeed(SimulationConfig.withImmunity(0.3), 5);
        run(a, 700);
        SimulationMemento m = a.createMemento();
        run(a, 800);

        Simulation b = Simulation.fromMemento(m);
        run(b, 800);

        assertEquals(a.view(), b.view());
    }

    // ---- what a memento contains

    @Test
    void mementoDescribesTheCurrentSituation() {
        Simulation simulation = Simulation.withSeed(SimulationConfig.noImmunity(), 3);
        run(simulation, 321);
        WorldView view = simulation.view();

        SimulationMemento m = simulation.createMemento();

        assertEquals(SimulationConfig.noImmunity(), m.config());
        assertEquals(321, m.tick());
        assertEquals(view.persons().size(), m.persons().size());
        for (int i = 0; i < m.persons().size(); i++) {
            PersonView shown = view.persons().get(i);
            var saved = m.persons().get(i);
            assertEquals(shown.id(), saved.id());
            assertEquals(shown.x(), saved.x(), 0);
            assertEquals(shown.y(), saved.y(), 0);
            assertEquals(shown.kind(), saved.kind());
        }
        assertTrue(m.nextPersonId() >= 60);
    }

    @Test
    void mementoTakenAtStartHoldsTheInitialPopulation() {
        Simulation simulation = Simulation.withSeed(SimulationConfig.noImmunity(), 1);

        SimulationMemento m = simulation.createMemento();

        assertEquals(0, m.tick());
        assertEquals(60, m.persons().size());
        assertEquals(60, m.nextPersonId());
        assertTrue(m.contacts().isEmpty());
    }

    @Test
    void equalSituationsGiveEqualMementosAndDifferentOnesDoNot() {
        Simulation a = Simulation.withSeed(SimulationConfig.noImmunity(), 9);
        Simulation b = Simulation.withSeed(SimulationConfig.noImmunity(), 9);
        run(a, 400);
        run(b, 400);
        assertEquals(a.createMemento(), b.createMemento());

        b.step();
        assertNotEquals(a.createMemento(), b.createMemento());
    }

    // ---- immutability and independence

    @Test
    void laterStepsDoNotChangeAnExistingMemento() {
        Simulation simulation = Simulation.withSeed(SimulationConfig.noImmunity(), 4);
        run(simulation, 300);
        SimulationMemento taken = simulation.createMemento();
        Simulation reference = Simulation.withSeed(SimulationConfig.noImmunity(), 4);
        run(reference, 300);

        run(simulation, 1000);

        assertEquals(reference.createMemento(), taken);
    }

    @Test
    void takingAMementoDoesNotInfluenceTheRun() {
        Simulation withMementos = Simulation.withSeed(SimulationConfig.noImmunity(), 6);
        Simulation without = Simulation.withSeed(SimulationConfig.noImmunity(), 6);

        for (int i = 0; i < 600; i++) {
            withMementos.step();
            withMementos.createMemento();
            without.step();
        }

        assertEquals(without.view(), withMementos.view());
        assertEquals(without.createMemento(), withMementos.createMemento());
    }

    @Test
    void restoringDoesNotChangeTheMementoAndItCanBeRestoredAgain() {
        Simulation source = Simulation.withSeed(SimulationConfig.noImmunity(), 8);
        run(source, 500);
        SimulationMemento m = source.createMemento();
        SimulationMemento copy = Simulation.withSeed(SimulationConfig.noImmunity(), 8).createMemento();
        assertNotEquals(copy, m);

        Simulation target = Simulation.withSeed(SimulationConfig.noImmunity(), 99);
        target.restore(m);
        run(target, 400);
        target.restore(m);
        run(target, 400);

        Simulation expected = Simulation.fromMemento(m);
        run(expected, 400);
        assertEquals(expected.view(), target.view());
        assertEquals(500, m.tick());
        assertEquals(500, Simulation.fromMemento(m).view().tick());
    }

    // ---- restore on other instances

    @Test
    void restoreWorksOnAFreshInstanceWithAnotherSeedAndConfiguration() {
        Simulation a = Simulation.withSeed(SimulationConfig.noImmunity(), 11);
        run(a, 450);
        SimulationMemento m = a.createMemento();
        run(a, 300);

        SimulationConfig other = new SimulationConfig(50, 20, 10, 2, 0.5, 0.5, 0.5, 0.5, 0.1, 1.0, 0.2);
        Simulation b = Simulation.withSeed(other, 12345);
        b.restore(m);
        assertEquals(SimulationConfig.noImmunity(), b.createMemento().config());
        assertEquals(30, b.view().areaWidth(), 0);
        run(b, 300);

        assertEquals(a.view(), b.view());
    }

    @Test
    void restoreWorksAfterThePopulationHasChanged() {
        Simulation a = Simulation.withSeed(SimulationConfig.noImmunity(), 13);
        run(a, 100);
        SimulationMemento early = a.createMemento();
        run(a, 3000);
        long idsLater = a.createMemento().nextPersonId();
        assertTrue(idsLater > early.nextPersonId(), "somebody should have entered in the meantime");

        a.restore(early);

        assertEquals(early.persons().size(), a.view().persons().size());
        assertEquals(early.nextPersonId(), a.createMemento().nextPersonId());
        assertEquals(100, a.view().tick());
    }

    @Test
    void restoringImmediatelyGivesTheSameMementoAgain() {
        Simulation simulation = Simulation.withSeed(SimulationConfig.noImmunity(), 14);
        run(simulation, 800);
        SimulationMemento m = simulation.createMemento();

        simulation.restore(m);

        assertEquals(m, simulation.createMemento());
    }

    @Test
    void restoreOverwritesTheRandomGeneratorState() {
        ScriptedRng rng = ScriptedRng.repeatingLast(0.5);
        Simulation scripted = new Simulation(SimulationConfig.noImmunity(), rng);
        Simulation source = Simulation.withSeed(SimulationConfig.noImmunity(), 1);
        SimulationMemento m = source.createMemento();

        scripted.restore(m);

        assertEquals(m.rngState(), rng.state());
    }
}
