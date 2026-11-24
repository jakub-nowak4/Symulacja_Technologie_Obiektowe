package lab3.ui.run;

import lab3.core.view.Stats;
import lab3.ui.run.StatsHistory.Sample;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StatsTextTest {
    @Test
    void thePolishWordFollowsTheNumber() {
        assertEquals("0 osób", StatsText.people(0));
        assertEquals("1 osoba", StatsText.people(1));
        assertEquals("2 osoby", StatsText.people(2));
        assertEquals("4 osoby", StatsText.people(4));
        assertEquals("5 osób", StatsText.people(5));
        assertEquals("11 osób", StatsText.people(11));
        assertEquals("12 osób", StatsText.people(12));
        assertEquals("14 osób", StatsText.people(14));
        assertEquals("21 osób", StatsText.people(21));
        assertEquals("22 osoby", StatsText.people(22));
        assertEquals("24 osoby", StatsText.people(24));
        assertEquals("25 osób", StatsText.people(25));
        assertEquals("112 osób", StatsText.people(112));
        assertEquals("102 osoby", StatsText.people(102));
    }

    @Test
    void aNegativeNumberIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> StatsText.people(-1));
    }

    @Test
    void thePeakLineNamesTheNumberAndTheTime() {
        Sample sample = new Sample(2050, new Stats(40, 3, 4, 13));   // 7 infected, 82 s

        assertEquals("Szczyt zakażeń: 7 osób (t = 82 s).", StatsText.describePeak(Optional.of(sample)));
        assertEquals("Szczyt zakażeń: 1 osoba (t = 1 s).",
                StatsText.describePeak(Optional.of(new Sample(25, new Stats(59, 0, 1, 0)))));
    }

    @Test
    void withoutInfectionsThePeakLineSaysSo() {
        String none = "Szczyt zakażeń: jeszcze nie było zakażonych.";

        assertEquals(none, StatsText.describePeak(Optional.empty()));
        assertEquals(none, StatsText.describePeak(Optional.of(new Sample(0, new Stats(60, 0, 0, 0)))));
    }
}
