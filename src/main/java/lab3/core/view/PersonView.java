package lab3.core.view;

import lab3.core.health.HealthKind;

/** Read-only picture of one person, as needed for drawing. */
public record PersonView(int id, double x, double y, HealthKind kind) {

    public PersonView {
        if (kind == null) {
            throw new IllegalArgumentException("kind must not be null");
        }
    }
}
