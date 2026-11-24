package lab3.core;

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
}
