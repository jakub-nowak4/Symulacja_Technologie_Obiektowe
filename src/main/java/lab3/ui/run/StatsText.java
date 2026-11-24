package lab3.ui.run;

import lab3.ui.run.StatsHistory.Sample;

import java.util.Locale;
import java.util.Optional;

/** The words under the chart; plain Java so that they can be tested without a window. */
public final class StatsText {
    private StatsText() {
    }

    /** "1 osoba", "2 osoby", "5 osób", "12 osób", "22 osoby". */
    public static String people(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count must not be negative: " + count);
        }
        String word;
        int lastDigit = count % 10;
        int lastTwoDigits = count % 100;
        if (count == 1) {
            word = "osoba";
        } else if (lastDigit >= 2 && lastDigit <= 4 && !(lastTwoDigits >= 12 && lastTwoDigits <= 14)) {
            word = "osoby";
        } else {
            word = "osób";
        }
        return count + " " + word;
    }

    /** The line about the highest number of infected people, given the peak sample (if there is one). */
    public static String describePeak(Optional<Sample> peak) {
        if (peak.isEmpty() || peak.get().stats().infected() == 0) {
            return "Szczyt zakażeń: jeszcze nie było zakażonych.";
        }
        Sample sample = peak.get();
        return String.format(Locale.ROOT, "Szczyt zakażeń: %s (t = %.0f s).",
                people(sample.stats().infected()), sample.seconds());
    }
}
