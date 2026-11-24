package lab3.core.health;

/**
 * State of the State pattern: how a person's health influences its behaviour. The person is the
 * context and replaces its state with the one returned by {@link #infect(boolean)} and
 * {@link #afterTick(long)}.
 *
 * <p>Hierarchy: immune XOR susceptible; a susceptible person is healthy XOR infected; an infected
 * person has symptoms XOR does not. States are stateless and shared; the remaining length of an
 * infection is kept by the person.
 */
public interface HealthState {

    HealthKind kind();

    /** Can this person catch the infection? True only for healthy, non-immune people. */
    boolean isSusceptible();

    /** Can this person pass the infection on? */
    boolean isInfectious();

    /** Probability of infecting a susceptible person who stays close enough long enough. */
    double transmissionProbability();

    /** Returns the state after catching the infection; people who cannot catch it keep their state. */
    HealthState infect(boolean symptomatic);

    /**
     * Returns the state for the next tick.
     *
     * @param infectionTicksLeft ticks of infection that remain after the current tick
     */
    HealthState afterTick(long infectionTicksLeft);

    /** Returns the shared state instance for the given kind (used when restoring from a memento). */
    static HealthState of(HealthKind kind) {
        return switch (kind) {
            case HEALTHY -> HealthyState.INSTANCE;
            case IMMUNE -> ImmuneState.INSTANCE;
            case INFECTED_ASYMPTOMATIC -> InfectedAsymptomaticState.INSTANCE;
            case INFECTED_SYMPTOMATIC -> InfectedSymptomaticState.INSTANCE;
        };
    }
}
