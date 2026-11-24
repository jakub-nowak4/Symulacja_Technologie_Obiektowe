package lab3.core;

/**
 * Parameters of a simulation run. Constants fixed by the lab specification are public static
 * fields; everything the specification leaves open is a record component (see assumptions
 * A1-A14 in docs/PLAN.md).
 *
 * @param areaWidth                  width of the area in metres
 * @param areaHeight                 height of the area in metres
 * @param initialPopulation          number of people at tick 0
 * @param initialInfected            how many of the initial people start infected (A6)
 * @param entryRatePerSecond         expected number of people entering per simulated second
 * @param minSpeed                   lower bound of a person's speed in m/s (A7)
 * @param immuneProbability          probability that an initial or entering person is immune (A5)
 * @param symptomaticProbability     probability that a new infection shows symptoms (A4)
 * @param velocityChangeProbability  per-tick probability of a random change of speed and heading (A8)
 * @param maxTurnAngle               largest heading change in radians per random change (A8)
 * @param maxSpeedDelta              largest speed change in m/s per random change (A8)
 */
public record SimulationConfig(
        double areaWidth,
        double areaHeight,
        int initialPopulation,
        int initialInfected,
        double entryRatePerSecond,
        double minSpeed,
        double immuneProbability,
        double symptomaticProbability,
        double velocityChangeProbability,
        double maxTurnAngle,
        double maxSpeedDelta) {

    /** Upper limit of speed in m/s. */
    public static final double MAX_SPEED = 2.5;
    /** Maximum distance in metres at which an infection can spread (inclusive). */
    public static final double CONTACT_RADIUS = 2.0;
    /** Number of consecutive ticks in contact needed to spread an infection (3 s). */
    public static final long CONTACT_TICKS = Ticks.fromSeconds(3);
    /** Shortest infection duration in ticks (20 s). */
    public static final long INFECTION_MIN_TICKS = Ticks.fromSeconds(20);
    /** Longest infection duration in ticks (30 s). */
    public static final long INFECTION_MAX_TICKS = Ticks.fromSeconds(30);
    /** Probability that a person entering the area is infected. */
    public static final double ENTRY_INFECTED_PROBABILITY = 0.1;
    /** Probability that a person reaching the border turns back instead of leaving. */
    public static final double BOUNCE_PROBABILITY = 0.5;
    /** Probability of catching the infection from an infected person without symptoms. */
    public static final double TRANSMISSION_ASYMPTOMATIC = 0.5;
    /** Probability of catching the infection from an infected person with symptoms. */
    public static final double TRANSMISSION_SYMPTOMATIC = 1.0;

    public SimulationConfig {
        requirePositiveFinite(areaWidth, "areaWidth");
        requirePositiveFinite(areaHeight, "areaHeight");
        if (initialPopulation < 0) {
            throw new IllegalArgumentException("initialPopulation must not be negative: " + initialPopulation);
        }
        if (initialInfected < 0 || initialInfected > initialPopulation) {
            throw new IllegalArgumentException(
                    "initialInfected must be between 0 and initialPopulation: " + initialInfected);
        }
        if (!(entryRatePerSecond >= 0) || Double.isInfinite(entryRatePerSecond)) {
            throw new IllegalArgumentException("entryRatePerSecond must be finite and not negative: " + entryRatePerSecond);
        }
        if (entryRatePerSecond * Ticks.DT > 1.0) {
            throw new IllegalArgumentException(
                    "entryRatePerSecond must not exceed one entry per tick (" + Ticks.PER_SECOND + "/s): " + entryRatePerSecond);
        }
        if (!(minSpeed > 0) || minSpeed > MAX_SPEED) {
            throw new IllegalArgumentException("minSpeed must be in (0, " + MAX_SPEED + "]: " + minSpeed);
        }
        requireProbability(immuneProbability, "immuneProbability");
        requireProbability(symptomaticProbability, "symptomaticProbability");
        requireProbability(velocityChangeProbability, "velocityChangeProbability");
        if (!(maxTurnAngle >= 0) || maxTurnAngle > Math.PI) {
            throw new IllegalArgumentException("maxTurnAngle must be in [0, pi]: " + maxTurnAngle);
        }
        if (!(maxSpeedDelta >= 0) || Double.isInfinite(maxSpeedDelta)) {
            throw new IllegalArgumentException("maxSpeedDelta must be finite and not negative: " + maxSpeedDelta);
        }
    }

    /**
     * Scenario 1: nobody is immune.
     *
     * <p>Calibrated in stage 17 (docs/PLAN.md, section 5): in a 30 x 30 m area, 60 people at the start and
     * 1.7 entries per second the population stays around 60 (mean 60.8, between 32 and 88 over 30 runs of
     * 10 minutes) and the infection keeps going without dying out (about 17 infected people at the peak).
     */
    public static SimulationConfig noImmunity() {
        return new SimulationConfig(30, 30, 60, 0, 1.7, 0.3, 0.0, 0.5, 0.02, Math.PI / 4, 0.5);
    }

    /** Scenario 2: the given share of the initial population and of entering people is immune. */
    public static SimulationConfig withImmunity(double immuneProbability) {
        return noImmunity().withImmuneProbability(immuneProbability);
    }

    public SimulationConfig withImmuneProbability(double newImmuneProbability) {
        return new SimulationConfig(areaWidth, areaHeight, initialPopulation, initialInfected, entryRatePerSecond,
                minSpeed, newImmuneProbability, symptomaticProbability, velocityChangeProbability,
                maxTurnAngle, maxSpeedDelta);
    }

    /** Probability that one new person enters the area in a single tick. */
    public double entryProbabilityPerTick() {
        return entryRatePerSecond * Ticks.DT;
    }

    private static void requirePositiveFinite(double value, String name) {
        if (!(value > 0) || Double.isInfinite(value)) {
            throw new IllegalArgumentException(name + " must be positive and finite: " + value);
        }
    }

    private static void requireProbability(double value, String name) {
        if (!(value >= 0 && value <= 1)) {
            throw new IllegalArgumentException(name + " must be in [0, 1]: " + value);
        }
    }
}
