package lab3.core.view;

import lab3.core.health.HealthKind;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorldViewTest {
    private static final double DELTA = 1e-12;

    private static PersonView person(int id, HealthKind kind) {
        return new PersonView(id, 1.0, 2.0, kind);
    }

    @Test
    void secondsConvertsTicksUsingTwentyFiveStepsPerSecond() {
        assertEquals(3.0, new WorldView(75, 30, 30, List.of()).seconds(), DELTA);
        assertEquals(0.0, new WorldView(0, 30, 30, List.of()).seconds(), DELTA);
    }

    @Test
    void statsAreDerivedFromPersons() {
        WorldView view = new WorldView(10, 30, 30, List.of(
                person(1, HealthKind.HEALTHY),
                person(2, HealthKind.INFECTED_SYMPTOMATIC),
                person(3, HealthKind.IMMUNE)));

        assertEquals(new Stats(1, 0, 1, 1), view.stats());
        assertEquals(3, view.stats().total());
    }

    @Test
    void keepsADefensiveCopyOfPersons() {
        List<PersonView> source = new ArrayList<>();
        source.add(person(1, HealthKind.HEALTHY));
        WorldView view = new WorldView(0, 30, 30, source);

        source.add(person(2, HealthKind.IMMUNE));

        assertEquals(1, view.persons().size());
    }

    @Test
    void personsListCannotBeModified() {
        WorldView view = new WorldView(0, 30, 30, List.of(person(1, HealthKind.HEALTHY)));

        assertThrows(UnsupportedOperationException.class, () -> view.persons().add(person(2, HealthKind.IMMUNE)));
    }

    @Test
    void rejectsInvalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> new WorldView(-1, 30, 30, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new WorldView(0, 0, 30, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new WorldView(0, 30, -5, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new WorldView(0, 30, 30, null));
    }
}
