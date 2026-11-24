package lab3.ui.run;

import lab3.core.SimulationConfig;

/**
 * What the user chooses when starting a new simulation: the scenario, the share of immune people
 * (used only in scenario 2) and the seed of the random number generator.
 */
public record RunSettings(Scenario scenario, double immuneProbability, long seed) {
    public static final double DEFAULT_IMMUNITY = 0.3;
    public static final long DEFAULT_SEED = 42;

    public RunSettings {
        if (scenario == null) {
            throw new IllegalArgumentException("scenario must not be null");
        }
        if (!(immuneProbability >= 0 && immuneProbability <= 1)) {
            throw new IllegalArgumentException("immuneProbability must be in [0, 1]: " + immuneProbability);
        }
    }

    /** Scenario 1 with the default seed. */
    public static RunSettings defaults() {
        return new RunSettings(Scenario.NO_IMMUNITY, DEFAULT_IMMUNITY, DEFAULT_SEED);
    }

    /** The configuration of the simulation for these settings; scenario 1 ignores the immune share. */
    public SimulationConfig config() {
        return switch (scenario) {
            case NO_IMMUNITY -> SimulationConfig.noImmunity();
            case WITH_IMMUNITY -> SimulationConfig.withImmunity(immuneProbability);
        };
    }

    /**
     * Reads a seed typed by the user.
     *
     * @throws IllegalArgumentException with a message for the user if the text is not a whole number
     */
    public static long parseSeed(String text) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Podaj ziarno (liczbę całkowitą).");
        }
        try {
            return Long.parseLong(trimmed);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Ziarno musi być liczbą całkowitą: " + trimmed);
        }
    }
}
