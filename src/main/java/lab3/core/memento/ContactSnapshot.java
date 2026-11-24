package lab3.core.memento;

import lab3.core.SimulationConfig;

/**
 * Frozen state of one pair in contact: how many consecutive ticks they have been within reach and
 * whether the transmission has already been drawn for this contact.
 *
 * <p>The draw is made as soon as the count reaches {@link SimulationConfig#CONTACT_TICKS}, so
 * {@code drawn} is always true exactly when {@code ticks} has reached that threshold.
 */
public record ContactSnapshot(int targetId, int sourceId, long ticks, boolean drawn) {

    public ContactSnapshot {
        if (targetId < 0 || sourceId < 0) {
            throw new IllegalArgumentException("person ids must not be negative");
        }
        if (targetId == sourceId) {
            throw new IllegalArgumentException("a person cannot be in contact with themselves: " + targetId);
        }
        if (ticks < 1) {
            throw new IllegalArgumentException("ticks must be at least 1: " + ticks);
        }
        if (drawn != ticks >= SimulationConfig.CONTACT_TICKS) {
            throw new IllegalArgumentException("drawn=" + drawn + " does not match ticks=" + ticks);
        }
    }
}
