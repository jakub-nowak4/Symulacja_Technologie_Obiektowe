package lab3.ui.run;

import lab3.core.SimulationEngine;
import lab3.core.Ticks;
import lab3.core.memento.SimulationMemento;
import lab3.core.memento.SnapshotHistory;

import java.util.ArrayList;
import java.util.List;

/**
 * What the window knows about mementos: two {@link SnapshotHistory} CareTakers of one simulation,
 * one for the mementos the user takes (or loads from a file) and one for the mementos taken
 * automatically at every whole second of simulated time.
 *
 * <p>The automatic ones are kept apart so that they can never push out the ones the user took:
 * they are many (at most {@link #AUTO_CAPACITY}, the oldest are dropped), the others few. All the
 * decisions - when an automatic memento is due, what is dropped, what a restore does to the lists -
 * are made here, without JavaFX, so that they can be tested.
 *
 * <p>Rules:
 * <ul>
 *   <li>An automatic memento is taken after the step that ends a whole second ({@link #afterStep()}
 *       must be called after every step, not once per frame, or seconds would be skipped). A second
 *       that already has one is not taken again, so going back and running on again adds nothing.</li>
 *   <li>Restoring an automatic memento keeps the automatic list: it comes from this very run, so the
 *       later ones are still valid and the user can go forward again.</li>
 *   <li>Restoring a memento taken by the user or loaded from a file empties the automatic list: it
 *       may come from another run, and the automatic mementos of this run would be misleading.</li>
 * </ul>
 */
public final class Snapshots {
    /** How many mementos taken by the user (or loaded from a file) are kept. */
    public static final int MANUAL_CAPACITY = 100;
    /** How many automatic mementos are kept: ten minutes of simulated time. */
    public static final int AUTO_CAPACITY = 600;

    public enum Kind {
        MANUAL("Ręczna"),
        AUTO("Auto");

        private final String title;

        Kind(String title) {
            this.title = title;
        }

        /** The word the list shows. */
        public String title() {
            return title;
        }
    }

    /** One line of the list: which memento it is and when in simulated time it was taken. */
    public record Item(Kind kind, int number, long tick, double seconds) {
        public Item {
            if (kind == null) {
                throw new IllegalArgumentException("kind must not be null");
            }
        }
    }

    private final SimulationEngine engine;
    private final int autoCapacity;
    private final SnapshotHistory manual;
    private SnapshotHistory auto;
    private boolean autoEnabled;
    private int version;

    public Snapshots(SimulationEngine engine) {
        this(engine, MANUAL_CAPACITY, AUTO_CAPACITY);
    }

    Snapshots(SimulationEngine engine, int manualCapacity, int autoCapacity) {
        this.engine = engine;
        this.autoCapacity = autoCapacity;
        this.manual = new SnapshotHistory(engine, manualCapacity);
        this.auto = new SnapshotHistory(engine, autoCapacity);
    }

    /** Takes a memento of the simulation as it is now. */
    public Item takeManual() {
        return addManual(engine.createMemento());
    }

    /** Keeps a memento that comes from elsewhere, for example from a file. */
    public Item addManual(SimulationMemento memento) {
        int number = manual.add(memento);
        version++;
        return new Item(Kind.MANUAL, number, memento.tick(), memento.seconds());
    }

    /**
     * Sends the simulation back to the moment of the memento; the memento stays in its list.
     *
     * @throws IllegalArgumentException if the memento is not kept any more (it may have been dropped)
     */
    public void restore(Item item) {
        requireItem(item);
        switch (item.kind()) {
            case MANUAL -> {
                manual.restore(item.number());
                auto = new SnapshotHistory(engine, autoCapacity);
            }
            case AUTO -> auto.restore(item.number());
        }
        version++;
    }

    /** Returns the memento itself, for example to save it to a file. */
    public SimulationMemento memento(Item item) {
        requireItem(item);
        return (item.kind() == Kind.MANUAL ? manual : auto).get(item.number());
    }

    /** Switches the automatic mementos on or off; switching on takes one at once if a whole second has just ended. */
    public void setAutoEnabled(boolean enabled) {
        autoEnabled = enabled;
        if (enabled) {
            takeAutoIfDue();
        }
    }

    public boolean autoEnabled() {
        return autoEnabled;
    }

    /** To be called after every single step of the simulation. */
    public void afterStep() {
        if (autoEnabled) {
            takeAutoIfDue();
        }
    }

    /** The mementos kept: first those of the user, then the automatic ones, each from the oldest. */
    public List<Item> items() {
        List<Item> items = new ArrayList<>();
        for (SnapshotHistory.Entry entry : manual.list()) {
            items.add(new Item(Kind.MANUAL, entry.number(), entry.tick(), entry.seconds()));
        }
        for (SnapshotHistory.Entry entry : auto.list()) {
            items.add(new Item(Kind.AUTO, entry.number(), entry.tick(), entry.seconds()));
        }
        return items;
    }

    /** Grows every time the list of items changes, so that a window can redraw it only when needed. */
    public int version() {
        return version;
    }

    private static void requireItem(Item item) {
        if (item == null) {
            throw new IllegalArgumentException("item must not be null");
        }
    }

    private void takeAutoIfDue() {
        long tick = engine.view().tick();
        if (tick % Ticks.PER_SECOND != 0) {
            return;
        }
        for (SnapshotHistory.Entry entry : auto.list()) {
            if (entry.tick() == tick) {
                return;
            }
        }
        auto.create();
        version++;
    }
}
