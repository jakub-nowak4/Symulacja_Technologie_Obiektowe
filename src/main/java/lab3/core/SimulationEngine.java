package lab3.core;

import lab3.core.memento.SimulationMemento;
import lab3.core.view.WorldView;

/**
 * The only part of the simulation that user interfaces and runners talk to. It exposes no
 * mutable domain objects.
 */
public interface SimulationEngine {

    /** Advances the simulation by exactly one tick ({@link Ticks#DT} seconds of simulated time). */
    void step();

    /** Returns an immutable snapshot of the current world. */
    WorldView view();

    /** Copies everything needed to continue from this very moment (Originator of the Memento pattern). */
    SimulationMemento createMemento();

    /**
     * Goes back to the moment of the memento: afterwards the simulation behaves exactly as the one the
     * memento was taken from would have. Works on any instance, whatever its current situation or
     * configuration. The memento is not changed and can be restored again.
     */
    void restore(SimulationMemento memento);
}
