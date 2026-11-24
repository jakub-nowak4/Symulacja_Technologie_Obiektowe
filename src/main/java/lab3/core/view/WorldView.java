package lab3.core.view;

import lab3.core.Ticks;

import java.util.List;

/**
 * Immutable snapshot of the world for drawing and statistics. It is not a memento: it cannot be
 * used to restore a simulation.
 */
public record WorldView(long tick, double areaWidth, double areaHeight, List<PersonView> persons) {

    public WorldView {
        if (tick < 0) {
            throw new IllegalArgumentException("tick must not be negative: " + tick);
        }
        if (!(areaWidth > 0) || !(areaHeight > 0)) {
            throw new IllegalArgumentException("area size must be positive: " + areaWidth + " x " + areaHeight);
        }
        if (persons == null) {
            throw new IllegalArgumentException("persons must not be null");
        }
        persons = List.copyOf(persons);
    }

    /** Simulated time in seconds, for display. */
    public double seconds() {
        return Ticks.toSeconds(tick);
    }

    public Stats stats() {
        return Stats.of(persons);
    }
}
