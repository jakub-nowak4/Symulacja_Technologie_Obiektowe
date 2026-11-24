package lab3.core;

/**
 * How a new infection looks: whether it shows symptoms (assumption A4) and how long it lasts
 * (20 to 30 seconds, uniformly, both ends included). Shared by everything that infects people.
 */
public final class InfectionPolicy {
    private final SimulationConfig config;

    public InfectionPolicy(SimulationConfig config) {
        this.config = config;
    }

    /**
     * Draws the symptoms (one draw) and the duration (one draw) of a new infection and applies it
     * to the person. Both draws are always made, so the number of draws does not depend on whether
     * the person can actually be infected.
     *
     * @return {@code true} if the person has just become infected
     */
    public boolean infect(Person person, Rng rng) {
        boolean symptomatic = rng.chance(config.symptomaticProbability());
        long span = SimulationConfig.INFECTION_MAX_TICKS - SimulationConfig.INFECTION_MIN_TICKS + 1;
        long duration = SimulationConfig.INFECTION_MIN_TICKS + rng.nextInt((int) span);
        return person.infect(symptomatic, duration);
    }
}
