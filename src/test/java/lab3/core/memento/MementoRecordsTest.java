package lab3.core.memento;

import lab3.core.Motion;
import lab3.core.Person;
import lab3.core.SimulationConfig;
import lab3.core.health.HealthKind;
import lab3.core.health.InfectedSymptomaticState;
import lab3.vectors.Vector2D;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class MementoRecordsTest {
    private static final long THRESHOLD = SimulationConfig.CONTACT_TICKS;

    private static PersonSnapshot healthy(int id) {
        return new PersonSnapshot(id, 1, 2, 0.5, -0.5, HealthKind.HEALTHY, 0);
    }

    private static PersonSnapshot infected(int id) {
        return new PersonSnapshot(id, 1, 2, 0.5, -0.5, HealthKind.INFECTED_SYMPTOMATIC, 600);
    }

    private static SimulationMemento memento(long tick, int nextId, List<PersonSnapshot> persons, List<ContactSnapshot> contacts) {
        return new SimulationMemento(SimulationConfig.noImmunity(), tick, 123, nextId, persons, contacts);
    }

    // ---- PersonSnapshot

    @Test
    void personSnapshotCopiesAPerson() {
        Person person = new Person(7, new Motion(new Vector2D(3, 4), new Vector2D(-1, 0.25)),
                InfectedSymptomaticState.INSTANCE, 520);

        PersonSnapshot snapshot = PersonSnapshot.of(person);

        assertEquals(new PersonSnapshot(7, 3, 4, -1, 0.25, HealthKind.INFECTED_SYMPTOMATIC, 520), snapshot);
    }

    @Test
    void personSnapshotCreatesAnIndependentPerson() {
        Person original = new Person(7, new Motion(new Vector2D(3, 4), new Vector2D(-1, 0.25)),
                InfectedSymptomaticState.INSTANCE, 520);
        PersonSnapshot snapshot = PersonSnapshot.of(original);

        Person copy = snapshot.toPerson();
        original.advanceHealth();
        original.setMotion(new Motion(new Vector2D(9, 9), new Vector2D(0, 0)));

        assertNotSame(original, copy);
        assertEquals(7, copy.id());
        assertEquals(3, copy.motion().position().getX(), 0);
        assertEquals(4, copy.motion().position().getY(), 0);
        assertEquals(-1, copy.motion().velocity().getX(), 0);
        assertEquals(0.25, copy.motion().velocity().getY(), 0);
        assertSame(InfectedSymptomaticState.INSTANCE, copy.state());
        assertEquals(520, copy.infectionTicksLeft());
    }

    @Test
    void personSnapshotRejectsInconsistentData() {
        assertThrows(IllegalArgumentException.class, () -> new PersonSnapshot(-1, 0, 0, 0, 0, HealthKind.HEALTHY, 0));
        assertThrows(IllegalArgumentException.class, () -> new PersonSnapshot(0, Double.NaN, 0, 0, 0, HealthKind.HEALTHY, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new PersonSnapshot(0, 0, Double.POSITIVE_INFINITY, 0, 0, HealthKind.HEALTHY, 0));
        assertThrows(IllegalArgumentException.class, () -> new PersonSnapshot(0, 0, 0, 0, 0, null, 0));
        assertThrows(IllegalArgumentException.class, () -> new PersonSnapshot(0, 0, 0, 0, 0, HealthKind.HEALTHY, 5));
        assertThrows(IllegalArgumentException.class, () -> new PersonSnapshot(0, 0, 0, 0, 0, HealthKind.IMMUNE, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new PersonSnapshot(0, 0, 0, 0, 0, HealthKind.INFECTED_ASYMPTOMATIC, 0));
    }

    // ---- ContactSnapshot

    @Test
    void contactSnapshotAcceptsConsistentPairs() {
        new ContactSnapshot(1, 2, 1, false);
        new ContactSnapshot(1, 2, THRESHOLD - 1, false);
        new ContactSnapshot(1, 2, THRESHOLD, true);
        new ContactSnapshot(1, 2, THRESHOLD + 100, true);
    }

    @Test
    void contactSnapshotRejectsInconsistentPairs() {
        assertThrows(IllegalArgumentException.class, () -> new ContactSnapshot(1, 2, THRESHOLD, false));
        assertThrows(IllegalArgumentException.class, () -> new ContactSnapshot(1, 2, THRESHOLD - 1, true));
        assertThrows(IllegalArgumentException.class, () -> new ContactSnapshot(1, 2, 0, false));
        assertThrows(IllegalArgumentException.class, () -> new ContactSnapshot(-1, 2, 5, false));
        assertThrows(IllegalArgumentException.class, () -> new ContactSnapshot(3, 3, 5, false));
    }

    // ---- SimulationMemento

    @Test
    void mementoKeepsItsContents() {
        SimulationMemento m = memento(250, 5, List.of(healthy(0), infected(3)), List.of(new ContactSnapshot(0, 3, 10, false)));

        assertEquals(250, m.tick());
        assertEquals(10.0, m.seconds(), 0);
        assertEquals(123, m.rngState());
        assertEquals(5, m.nextPersonId());
        assertEquals(2, m.persons().size());
        assertEquals(1, m.contacts().size());
        assertEquals(SimulationConfig.noImmunity(), m.config());
    }

    @Test
    void mementoIsNotAffectedByLaterChangesOfTheListsItWasBuiltFrom() {
        List<PersonSnapshot> persons = new ArrayList<>(List.of(healthy(0), healthy(1)));
        List<ContactSnapshot> contacts = new ArrayList<>();
        SimulationMemento m = memento(0, 2, persons, contacts);

        persons.add(healthy(2));
        contacts.add(new ContactSnapshot(0, 1, 3, false));

        assertEquals(2, m.persons().size());
        assertEquals(0, m.contacts().size());
    }

    @Test
    void mementoListsCannotBeModified() {
        SimulationMemento m = memento(0, 2, List.of(healthy(0)), List.of());

        assertThrows(UnsupportedOperationException.class, () -> m.persons().add(healthy(1)));
        assertThrows(UnsupportedOperationException.class, () -> m.contacts().add(new ContactSnapshot(0, 1, 1, false)));
    }

    @Test
    void mementosWithTheSameContentsAreEqual() {
        SimulationMemento a = memento(5, 2, List.of(healthy(0), healthy(1)), List.of());
        SimulationMemento b = memento(5, 2, List.of(healthy(0), healthy(1)), List.of());

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void mementoRejectsWrongArguments() {
        List<PersonSnapshot> two = List.of(healthy(0), healthy(1));
        assertThrows(IllegalArgumentException.class, () -> new SimulationMemento(null, 0, 0, 2, two, List.of()));
        assertThrows(IllegalArgumentException.class, () -> memento(-1, 2, two, List.of()));
        assertThrows(IllegalArgumentException.class, () -> memento(0, 2, null, List.of()));
        assertThrows(IllegalArgumentException.class, () -> memento(0, 2, two, null));
    }

    @Test
    void mementoRejectsUnsortedOrDuplicatedPeople() {
        assertThrows(IllegalArgumentException.class, () -> memento(0, 5, List.of(healthy(2), healthy(1)), List.of()));
        assertThrows(IllegalArgumentException.class, () -> memento(0, 5, List.of(healthy(1), healthy(1)), List.of()));
    }

    @Test
    void mementoRejectsNextIdThatCouldClash() {
        assertThrows(IllegalArgumentException.class, () -> memento(0, 1, List.of(healthy(0), healthy(1)), List.of()));
        memento(0, 2, List.of(healthy(0), healthy(1)), List.of());
    }

    @Test
    void mementoRejectsContactsOfUnknownPeople() {
        List<PersonSnapshot> people = List.of(healthy(0), infected(1));

        assertThrows(IllegalArgumentException.class,
                () -> memento(0, 2, people, List.of(new ContactSnapshot(0, 9, 3, false))));
        assertThrows(IllegalArgumentException.class,
                () -> memento(0, 2, people, List.of(new ContactSnapshot(9, 1, 3, false))));
    }

    @Test
    void mementoRejectsUnsortedOrDuplicatedContacts() {
        List<PersonSnapshot> people = List.of(healthy(0), healthy(1), infected(2), infected(3));
        ContactSnapshot a = new ContactSnapshot(0, 2, 3, false);
        ContactSnapshot b = new ContactSnapshot(0, 3, 3, false);
        ContactSnapshot c = new ContactSnapshot(1, 2, 3, false);

        memento(0, 4, people, List.of(a, b, c));
        assertThrows(IllegalArgumentException.class, () -> memento(0, 4, people, List.of(b, a)));
        assertThrows(IllegalArgumentException.class, () -> memento(0, 4, people, List.of(c, a)));
        assertThrows(IllegalArgumentException.class, () -> memento(0, 4, people, List.of(a, a)));
    }
}
