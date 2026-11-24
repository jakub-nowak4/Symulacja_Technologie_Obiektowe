package lab3.core;

import lab3.core.health.HealthKind;
import lab3.core.health.HealthState;
import lab3.core.health.HealthyState;
import lab3.core.view.PersonView;

/**
 * One person in the simulation: the context of the State pattern. Movement is described by a
 * {@link Motion}; health behaviour is delegated to the current {@link HealthState}.
 *
 * <p>The remaining length of an infection is kept here, not in the (shared, stateless) states.
 */
public final class Person {
    private final int id;
    private Motion motion;
    private HealthState state;
    private long infectionTicksLeft;

    /** Creates a person who is not infected (healthy or immune). */
    public Person(int id, Motion motion, HealthState state) {
        this(id, motion, state, 0);
    }

    /**
     * Creates a person in any state; used to restore a person from a memento.
     *
     * @param infectionTicksLeft remaining infection length, positive for infected people and zero otherwise
     */
    public Person(int id, Motion motion, HealthState state, long infectionTicksLeft) {
        if (motion == null || state == null) {
            throw new IllegalArgumentException("motion and state must not be null");
        }
        if (state.isInfectious() ? infectionTicksLeft < 1 : infectionTicksLeft != 0) {
            throw new IllegalArgumentException(
                    "infectionTicksLeft " + infectionTicksLeft + " does not match state " + state.kind());
        }
        this.id = id;
        this.motion = motion;
        this.state = state;
        this.infectionTicksLeft = infectionTicksLeft;
    }

    /** Creates a healthy person. */
    public static Person healthy(int id, Motion motion) {
        return new Person(id, motion, HealthyState.INSTANCE);
    }

    public int id() {
        return id;
    }

    public Motion motion() {
        return motion;
    }

    public void setMotion(Motion motion) {
        if (motion == null) {
            throw new IllegalArgumentException("motion must not be null");
        }
        this.motion = motion;
    }

    public HealthKind kind() {
        return state.kind();
    }

    public HealthState state() {
        return state;
    }

    public long infectionTicksLeft() {
        return infectionTicksLeft;
    }

    public boolean isSusceptible() {
        return state.isSusceptible();
    }

    public boolean isInfectious() {
        return state.isInfectious();
    }

    /**
     * Tries to infect this person. Succeeds only for susceptible people (healthy and not immune).
     *
     * @param symptomatic   whether the new infection shows symptoms
     * @param durationTicks length of the infection in ticks, at least 1
     * @return {@code true} if the person has just become infected
     */
    public boolean infect(boolean symptomatic, long durationTicks) {
        if (durationTicks < 1) {
            throw new IllegalArgumentException("durationTicks must be at least 1: " + durationTicks);
        }
        HealthState next = state.infect(symptomatic);
        if (next == state) {
            return false;
        }
        state = next;
        infectionTicksLeft = durationTicks;
        return true;
    }

    /**
     * Advances the health of this person by one tick: an infected person has one tick less to go
     * and becomes immune when none are left.
     */
    public void advanceHealth() {
        if (state.isInfectious()) {
            infectionTicksLeft--;
        }
        state = state.afterTick(infectionTicksLeft);
        if (!state.isInfectious()) {
            infectionTicksLeft = 0;
        }
    }

    public PersonView view() {
        return new PersonView(id, motion.position().getX(), motion.position().getY(), state.kind());
    }
}
