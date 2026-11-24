package lab3.ui.draw;

import lab3.core.health.HealthKind;

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
