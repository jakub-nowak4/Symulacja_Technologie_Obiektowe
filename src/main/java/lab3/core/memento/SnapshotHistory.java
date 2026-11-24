package lab3.core.memento;

import lab3.core.SimulationEngine;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The CareTaker of the Memento pattern: keeps mementos of one simulation, numbered in the order in
 * which they were added, and gives them back on request. It never looks inside a memento.
 *
 * <p>Numbers start at 1 and are never used again, also after old mementos have been dropped. When
 * the capacity is reached the oldest memento is dropped to make room (so that taking a memento
 * every simulated second does not use memory without limit).
 */
public final class SnapshotHistory {

    /** What the list of mementos shows about one of them. */
    public record Entry(int number, long tick, double seconds) {
    }

    private final SimulationEngine originator;
    private final int capacity;
    private final Map<Integer, SimulationMemento> mementos = new LinkedHashMap<>();
    private int nextNumber = 1;

    /**
     * @param originator the simulation whose mementos are kept
     * @param capacity   how many mementos are kept at most, at least 1
     */
    public SnapshotHistory(SimulationEngine originator, int capacity) {
        if (originator == null) {
            throw new IllegalArgumentException("originator must not be null");
        }
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be at least 1: " + capacity);
        }
        this.originator = originator;
        this.capacity = capacity;
    }

    /** Takes a memento of the simulation as it is now and returns its number. */
    public int create() {
        return add(originator.createMemento());
    }

    /** Keeps a memento that comes from elsewhere, for example from a file, and returns its number. */
    public int add(SimulationMemento memento) {
        if (memento == null) {
            throw new IllegalArgumentException("memento must not be null");
        }
        if (mementos.size() == capacity) {
            Iterator<Integer> oldest = mementos.keySet().iterator();
            oldest.next();
            oldest.remove();
        }
        int number = nextNumber++;
        mementos.put(number, memento);
        return number;
    }

    /** Sends the simulation back to the moment of memento number {@code number}; the memento stays. */
    public void restore(int number) {
        originator.restore(get(number));
    }

    /** Returns memento number {@code number}, for example to save it to a file. */
    public SimulationMemento get(int number) {
        SimulationMemento memento = mementos.get(number);
        if (memento == null) {
            throw new IllegalArgumentException("there is no memento number " + number + " (kept: " + describeKept() + ")");
        }
        return memento;
    }

    /** Lists the mementos that are kept, from the oldest to the newest. */
    public List<Entry> list() {
        List<Entry> entries = new ArrayList<>(mementos.size());
        for (Map.Entry<Integer, SimulationMemento> item : mementos.entrySet()) {
            SimulationMemento memento = item.getValue();
            entries.add(new Entry(item.getKey(), memento.tick(), memento.seconds()));
        }
        return entries;
    }

    public int size() {
        return mementos.size();
    }

    public int capacity() {
        return capacity;
    }

    private String describeKept() {
        return mementos.isEmpty() ? "none" : mementos.keySet().iterator().next() + ".." + (nextNumber - 1);
    }
}
