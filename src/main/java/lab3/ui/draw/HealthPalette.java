package lab3.ui.draw;

import lab3.core.health.HealthKind;
import lab3.core.view.Stats;

import java.util.List;

/**
 * Colours and names of the health categories, shared by the picture and the legend. Colours are
 * plain {@code 0xRRGGBB} numbers so that this class does not depend on JavaFX.
 */
public final class HealthPalette {
    private HealthPalette() {
    }

    /** The order in which the legend lists the categories. */
    public static List<HealthKind> legendOrder() {
        return List.of(HealthKind.HEALTHY, HealthKind.INFECTED_ASYMPTOMATIC,
                HealthKind.INFECTED_SYMPTOMATIC, HealthKind.IMMUNE);
    }

    public static int rgb(HealthKind kind) {
        return switch (kind) {
            case HEALTHY -> 0x2E9E4F;               // green
            case INFECTED_SYMPTOMATIC -> 0xD62828;  // red
            case INFECTED_ASYMPTOMATIC -> 0xF4A100; // orange
            case IMMUNE -> 0x2F6FDE;                // blue
        };
    }

    /** The colour as text for style sheets, for example {@code #2E9E4F}. */
    public static String hex(HealthKind kind) {
        return String.format("#%06X", rgb(kind));
    }

    /** How many people of this category there are in the counts. */
    public static int count(Stats stats, HealthKind kind) {
        return switch (kind) {
            case HEALTHY -> stats.healthy();
            case INFECTED_ASYMPTOMATIC -> stats.infectedAsymptomatic();
            case INFECTED_SYMPTOMATIC -> stats.infectedSymptomatic();
            case IMMUNE -> stats.immune();
        };
    }

    /** Name shown in the legend. */
    public static String label(HealthKind kind) {
        return switch (kind) {
            case HEALTHY -> "Zdrowy";
            case INFECTED_SYMPTOMATIC -> "Zakażony (objawowy)";
            case INFECTED_ASYMPTOMATIC -> "Zakażony (bezobjawowy)";
            case IMMUNE -> "Odporny";
        };
    }
}
