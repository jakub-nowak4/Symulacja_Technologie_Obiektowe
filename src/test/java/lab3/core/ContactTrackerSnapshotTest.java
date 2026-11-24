package lab3.core;

import lab3.core.memento.ContactSnapshot;
import lab3.vectors.Vector2D;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContactTrackerSnapshotTest {
    private static final long THRESHOLD = SimulationConfig.CONTACT_TICKS;

    private static Person at(int id, double x, double y) {
        return Person.healthy(id, new Motion(new Vector2D(x, y), new Vector2D(0, 0)));
    }

    private static Person infectedAt(int id, double x, double y) {
        Person p = at(id, x, y);
        p.infect(true, 5000);
        return p;
    }

    private static void evaluate(ContactTracker tracker, List<Person> people, Rng rng, long times) {
        for (long i = 0; i < times; i++) {
            tracker.evaluate(people, rng);
        }
    }

    @Test
    void newTrackerHasNoContacts() {
        assertTrue(new ContactTracker().snapshot().isEmpty());
    }

    @Test
    void snapshotListsTheContactsInProgressSortedByTargetThenSource() {
        // targets 1 and 4 (healthy), sources 2 and 3 (infectious), all close together
        List<Person> people = List.of(at(4, 10, 10), infectedAt(3, 10.5, 10), at(1, 10, 11), infectedAt(2, 11, 10));
        ContactTracker tracker = new ContactTracker();

        evaluate(tracker, people, new SplitMix64Rng(1), 5);

        assertEquals(List.of(
                new ContactSnapshot(1, 2, 5, false), new ContactSnapshot(1, 3, 5, false),
                new ContactSnapshot(4, 2, 5, false), new ContactSnapshot(4, 3, 5, false)), tracker.snapshot());
    }

    @Test
    void snapshotMarksPairsThatHaveAlreadyBeenDrawn() {
        List<Person> people = List.of(at(0, 10, 10), infectedAt(1, 11, 10));
        ContactTracker tracker = new ContactTracker();

        evaluate(tracker, people, new ScriptedRng(0.99), THRESHOLD - 1);
        assertEquals(List.of(new ContactSnapshot(0, 1, THRESHOLD - 1, false)), tracker.snapshot());

        evaluate(tracker, people, new ScriptedRng(0.99), 1);
        assertEquals(List.of(new ContactSnapshot(0, 1, THRESHOLD, true)), tracker.snapshot());
    }

    @Test
    void restoredTrackerContinuesCountingAndDrawsExactlyOnceAtTheThreshold() {
        List<Person> people = List.of(at(0, 10, 10), infectedAt(1, 11, 10));
        ContactTracker original = new ContactTracker();
        evaluate(original, people, new ScriptedRng(0.0), THRESHOLD - 1);

        ContactTracker restored = new ContactTracker();
        restored.restore(original.snapshot());

        // one more tick reaches the threshold: the draw (0.0 < 1.0) infects
        List<Infection> infections = restored.evaluate(people, new ScriptedRng(0.0));
        assertEquals(List.of(new Infection(0, 1)), infections);
        assertEquals(List.of(new ContactSnapshot(0, 1, THRESHOLD, true)), restored.snapshot());
    }

    @Test
    void restoredTrackerDoesNotDrawAgainForAContactThatWasAlreadyDrawn() {
        List<Person> people = List.of(at(0, 10, 10), infectedAt(1, 11, 10));
        ContactTracker restored = new ContactTracker();
        restored.restore(List.of(new ContactSnapshot(0, 1, THRESHOLD + 10, true)));

        Rng forbidden = new Rng() {
            @Override
            public double nextDouble() {
                throw new AssertionError("no draw expected");
            }

            @Override
            public long state() {
                return 0;
            }

            @Override
            public void setState(long state) {
            }
        };

        List<Infection> infections = restored.evaluate(people, forbidden);

        assertTrue(infections.isEmpty());
        assertEquals(List.of(new ContactSnapshot(0, 1, THRESHOLD + 11, true)), restored.snapshot());
    }

    @Test
    void restoreReplacesWhateverWasThereBefore() {
        List<Person> people = List.of(at(0, 10, 10), infectedAt(1, 11, 10));
        ContactTracker tracker = new ContactTracker();
        evaluate(tracker, people, new SplitMix64Rng(1), 30);

        tracker.restore(List.of());

        assertTrue(tracker.snapshot().isEmpty());
    }

    @Test
    void restoreRejectsDuplicates() {
        ContactTracker tracker = new ContactTracker();

        assertThrows(IllegalArgumentException.class, () -> tracker.restore(
                List.of(new ContactSnapshot(0, 1, 3, false), new ContactSnapshot(0, 1, 4, false))));
    }

    @Test
    void snapshotIsACopy() {
        List<Person> people = List.of(at(0, 10, 10), infectedAt(1, 11, 10));
        ContactTracker tracker = new ContactTracker();
        evaluate(tracker, people, new SplitMix64Rng(1), 5);
        List<ContactSnapshot> before = tracker.snapshot();

        evaluate(tracker, people, new SplitMix64Rng(1), 5);

        assertEquals(List.of(new ContactSnapshot(0, 1, 5, false)), before);
        assertEquals(List.of(new ContactSnapshot(0, 1, 10, false)), tracker.snapshot());
    }
}
