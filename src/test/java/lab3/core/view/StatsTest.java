package lab3.core.view;

import lab3.core.health.HealthKind;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StatsTest {

    private static PersonView person(int id, HealthKind kind) {
        return new PersonView(id, 0, 0, kind);
    }

    @Test
    void ofCountsEachKind() {
        Stats stats = Stats.of(List.of(
                person(1, HealthKind.HEALTHY),
                person(2, HealthKind.HEALTHY),
                person(3, HealthKind.HEALTHY),
                person(4, HealthKind.INFECTED_ASYMPTOMATIC),
                person(5, HealthKind.INFECTED_SYMPTOMATIC),
                person(6, HealthKind.INFECTED_SYMPTOMATIC),
                person(7, HealthKind.IMMUNE)));

        assertEquals(new Stats(3, 1, 2, 1), stats);
    }

    @Test
    void ofEmptyListIsAllZeros() {
        assertEquals(new Stats(0, 0, 0, 0), Stats.of(List.of()));
    }

    @Test
    void infectedAndTotalAddUpTheCategories() {
        Stats stats = new Stats(10, 2, 3, 4);

        assertEquals(5, stats.infected());
        assertEquals(19, stats.total());
    }

    @Test
    void rejectsNegativeCounts() {
        assertThrows(IllegalArgumentException.class, () -> new Stats(-1, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new Stats(0, -1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new Stats(0, 0, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> new Stats(0, 0, 0, -1));
    }

    @Test
    void personViewRejectsNullKind() {
        assertThrows(IllegalArgumentException.class, () -> new PersonView(1, 0, 0, null));
    }
}
