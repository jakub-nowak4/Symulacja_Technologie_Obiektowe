package lab3.core.health;

import lab3.core.SimulationConfig;

/** Infected without symptoms: spreads the infection with probability 0.5. */
public final class InfectedAsymptomaticState extends InfectedState {
    public static final InfectedAsymptomaticState INSTANCE = new InfectedAsymptomaticState();

    private InfectedAsymptomaticState() {
    }

    @Override
    public HealthKind kind() {
        return HealthKind.INFECTED_ASYMPTOMATIC;
    }

    @Override
    public double transmissionProbability() {
        return SimulationConfig.TRANSMISSION_ASYMPTOMATIC;
    }
}
