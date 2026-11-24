package lab3.ui.draw;

import lab3.core.health.HealthKind;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HealthPaletteTest {

    @Test
    void everyKindHasItsOwnColourAndName() {
        Set<Integer> colours = new HashSet<>();
        Set<String> labels = new HashSet<>();

        for (HealthKind kind : HealthKind.values()) {
            assertTrue(HealthPalette.rgb(kind) >= 0 && HealthPalette.rgb(kind) <= 0xFFFFFF, kind.toString());
            assertTrue(!HealthPalette.label(kind).isBlank(), kind.toString());
            colours.add(HealthPalette.rgb(kind));
            labels.add(HealthPalette.label(kind));
        }

        assertEquals(HealthKind.values().length, colours.size());
        assertEquals(HealthKind.values().length, labels.size());
    }

    @Test
    void legendListsEveryKindExactlyOnce() {
        assertEquals(Set.copyOf(java.util.Arrays.asList(HealthKind.values())), Set.copyOf(HealthPalette.legendOrder()));
        assertEquals(HealthKind.values().length, HealthPalette.legendOrder().size());
        assertEquals(HealthKind.HEALTHY, HealthPalette.legendOrder().get(0));
    }

    @Test
    void colourMeaningFollowsThePlan() {
        int healthy = HealthPalette.rgb(HealthKind.HEALTHY);
        int symptomatic = HealthPalette.rgb(HealthKind.INFECTED_SYMPTOMATIC);
        int asymptomatic = HealthPalette.rgb(HealthKind.INFECTED_ASYMPTOMATIC);
        int immune = HealthPalette.rgb(HealthKind.IMMUNE);

        assertTrue(green(healthy) > red(healthy) && green(healthy) > blue(healthy), "healthy is green");
        assertTrue(red(symptomatic) > green(symptomatic) && red(symptomatic) > blue(symptomatic), "symptomatic is red");
        assertTrue(red(asymptomatic) > blue(asymptomatic) && green(asymptomatic) > blue(asymptomatic), "asymptomatic is orange");
        assertTrue(blue(immune) > red(immune) && blue(immune) > green(immune), "immune is blue");
    }

    private static int red(int rgb) {
        return (rgb >> 16) & 0xFF;
    }

    private static int green(int rgb) {
        return (rgb >> 8) & 0xFF;
    }

    private static int blue(int rgb) {
        return rgb & 0xFF;
    }
}
