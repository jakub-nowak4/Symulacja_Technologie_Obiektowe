package lab3.core;

import lab3.core.memento.ContactSnapshot;
import lab3.vectors.Vector2D;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Decides who catches the infection from whom (assumptions A1-A3 and A12 in docs/PLAN.md).
 *
 * <p>For every pair (susceptible person, infectious person) it counts consecutive ticks in which
 * they are at most {@link SimulationConfig#CONTACT_RADIUS} metres apart. Leaving the radius resets
 * the count. When the count reaches {@link SimulationConfig#CONTACT_TICKS} the transmission is
 * drawn <em>once</em> with the source's transmission probability; the pair is not drawn again until
 * the contact has been broken and re-established.
 *
 * <p>The states of all people are read as they are when {@link #evaluate} is called; the caller
 * applies the returned infections afterwards, so nobody infected in this tick spreads it in the
 * same tick. Pairs are processed in order of ids, so the outcome does not depend on the order of
 * the list that is passed in.
 */
public final class ContactTracker {
    private static final double RADIUS_SQUARED = SimulationConfig.CONTACT_RADIUS * SimulationConfig.CONTACT_RADIUS;

    /** Consecutive-contact state of one pair. */
    private static final class Contact {
        long ticks;
        boolean drawn;
    }

    /** Pairs currently in contact, ordered by (target id, source id). */
    private Map<Long, Contact> contacts = new TreeMap<>();

    /**
     * Advances all contact counters by one tick and returns the infections that happen now. Each
     * target appears at most once, with the lowest-id source whose draw succeeded; a draw is made
     * for every pair that reaches the threshold in this tick.
     */
    public List<Infection> evaluate(List<Person> persons, Rng rng) {
        List<Person> susceptible = new ArrayList<>();
        List<Person> infectious = new ArrayList<>();
        for (Person person : persons) {
            if (person.isSusceptible()) {
                susceptible.add(person);
            } else if (person.isInfectious()) {
                infectious.add(person);
            }
        }
        Comparator<Person> byId = Comparator.comparingInt(Person::id);
        susceptible.sort(byId);
        infectious.sort(byId);

        Map<Long, Contact> next = new TreeMap<>();
        Set<Integer> alreadyInfected = new HashSet<>();
        List<Infection> infections = new ArrayList<>();

        for (Person target : susceptible) {
            for (Person source : infectious) {
                if (!inContact(target, source)) {
                    continue;
                }
                long key = pairKey(target.id(), source.id());
                Contact contact = contacts.get(key);
                if (contact == null) {
                    contact = new Contact();
                }
                contact.ticks++;
                next.put(key, contact);

                if (!contact.drawn && contact.ticks >= SimulationConfig.CONTACT_TICKS) {
                    contact.drawn = true;
                    boolean transmitted = rng.chance(source.state().transmissionProbability());
                    if (transmitted && alreadyInfected.add(target.id())) {
                        infections.add(new Infection(target.id(), source.id()));
                    }
                }
            }
        }
        contacts = next;
        return infections;
    }

    /** Copies the contacts in progress, sorted by (target id, source id). */
    public List<ContactSnapshot> snapshot() {
        List<ContactSnapshot> result = new ArrayList<>(contacts.size());
        for (Map.Entry<Long, Contact> entry : contacts.entrySet()) {
            long key = entry.getKey();
            Contact contact = entry.getValue();
            result.add(new ContactSnapshot((int) (key >>> 32), (int) key, contact.ticks, contact.drawn));
        }
        return result;
    }

    /** Replaces the contacts in progress with the given ones. */
    public void restore(List<ContactSnapshot> snapshot) {
        Map<Long, Contact> restored = new TreeMap<>();
        for (ContactSnapshot item : snapshot) {
            Contact contact = new Contact();
            contact.ticks = item.ticks();
            contact.drawn = item.drawn();
            if (restored.put(pairKey(item.targetId(), item.sourceId()), contact) != null) {
                throw new IllegalArgumentException("duplicate contact " + item.targetId() + " / " + item.sourceId());
            }
        }
        contacts = restored;
    }

    private static boolean inContact(Person a, Person b) {
        Vector2D d = a.motion().position().minus(b.motion().position());
        return d.cdot(d) <= RADIUS_SQUARED;
    }

    private static long pairKey(int targetId, int sourceId) {
        if (targetId < 0 || sourceId < 0) {
            throw new IllegalArgumentException("person ids must not be negative");
        }
        return ((long) targetId << 32) | sourceId;
    }
}
