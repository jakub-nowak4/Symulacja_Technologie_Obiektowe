package lab3.ui;

import javafx.scene.paint.Color;
import lab3.core.health.HealthKind;
import lab3.ui.draw.HealthPalette;

/** Turns the plain colour numbers of {@link HealthPalette} into JavaFX colours. */
final class Colors {
    private Colors() {
    }

    static Color of(HealthKind kind) {
        return of(kind, 1.0);
    }

    static Color of(HealthKind kind, double opacity) {
        int rgb = HealthPalette.rgb(kind);
        return Color.rgb((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, opacity);
    }
}
