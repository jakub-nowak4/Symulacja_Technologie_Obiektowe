package lab3.core.memento;

import lab3.core.SimulationConfig;
import lab3.core.Ticks;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The Memento of the Memento pattern: everything needed to continue a simulation exactly where it
 * was, and nothing else. It is immutable and holds copies only, so later steps of the simulation
 * cannot change it, and it can be restored any number of times.
 *
 * <p>Contents: the configuration (so a memento is self-contained), the tick, the state of the
 * random number generator, the next free person id, all people and all contacts in progress.
 * People are sorted by ascending id and contacts by (target id, source id); the constructor checks
 * this, so equal situations give equal mementos.
 *
 * <p>Only the originator ({@code Simulation}) creates and reads mementos. It is not a view: user
 * interfaces draw from {@code WorldView}.
 */
public record SimulationMemento(
        SimulationConfig config,
        long tick,
        long rngState,
        int nextPersonId,
        List<PersonSnapshot> persons,
        List<ContactSnapshot> contacts) {

    public SimulationMemento {
        if (config == null || persons == null || contacts == null) {
            throw new IllegalArgumentException("config, persons and contacts must not be null");
        }
        if (tick < 0) {
            throw new IllegalArgumentException("tick must not be negative: " + tick);
        }
        persons = List.copyOf(persons);
        contacts = List.copyOf(contacts);

        Set<Integer> ids = new HashSet<>();
        int previousId = -1;
        for (PersonSnapshot person : persons) {
            if (person.id() <= previousId) {
                throw new IllegalArgumentException("persons must have strictly ascending ids, got " + person.id()
                        + " after " + previousId);
            }
            previousId = person.id();
            ids.add(person.id());
        }
        if (nextPersonId <= previousId) {
            throw new IllegalArgumentException("nextPersonId " + nextPersonId + " must be greater than every person id");
        }

        long previousKey = -1;
        for (ContactSnapshot contact : contacts) {
            if (!ids.contains(contact.targetId()) || !ids.contains(contact.sourceId())) {
                throw new IllegalArgumentException("contact refers to a person who does not exist: "
                        + contact.targetId() + " / " + contact.sourceId());
            }
            long key = ((long) contact.targetId() << 32) | contact.sourceId();
            if (key <= previousKey) {
                throw new IllegalArgumentException("contacts must be sorted by (target, source) without duplicates");
            }
            previousKey = key;
        }
    }

    /** Simulated time in seconds at which the memento was taken. */
    public double seconds() {
        return Ticks.toSeconds(tick);
    }
}
