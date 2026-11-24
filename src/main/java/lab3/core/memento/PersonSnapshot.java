package lab3.core.memento;

import lab3.core.Motion;
import lab3.core.Person;
import lab3.core.health.HealthKind;
import lab3.core.health.HealthState;
import lab3.vectors.Vector2D;

/**
 * Frozen copy of one person: plain numbers only, no reference to a live object.
 *
 * @param infectionTicksLeft remaining infection length; at least 1 for infected people, 0 for the others
 */
public record PersonSnapshot(int id, double x, double y, double vx, double vy, HealthKind kind, long infectionTicksLeft) {

    public PersonSnapshot {
        if (id < 0) {
            throw new IllegalArgumentException("id must not be negative: " + id);
        }
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(vx) || !Double.isFinite(vy)) {
            throw new IllegalArgumentException("position and velocity must be finite numbers");
        }
        if (kind == null) {
            throw new IllegalArgumentException("kind must not be null");
        }
        if (kind.isInfected() ? infectionTicksLeft < 1 : infectionTicksLeft != 0) {
            throw new IllegalArgumentException("infectionTicksLeft " + infectionTicksLeft + " does not match " + kind);
        }
    }

    /** Copies the current state of a person. */
    public static PersonSnapshot of(Person person) {
        Motion motion = person.motion();
        return new PersonSnapshot(person.id(),
                motion.position().getX(), motion.position().getY(),
                motion.velocity().getX(), motion.velocity().getY(),
                person.kind(), person.infectionTicksLeft());
    }

    /** Creates a new, independent person from this snapshot. */
    public Person toPerson() {
        Motion motion = new Motion(new Vector2D(x, y), new Vector2D(vx, vy));
        return new Person(id, motion, HealthState.of(kind), infectionTicksLeft);
    }
}
