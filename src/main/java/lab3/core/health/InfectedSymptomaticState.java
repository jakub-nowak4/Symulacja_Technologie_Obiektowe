package lab3.core.health;

import lab3.core.SimulationConfig;

/** Infected with symptoms: spreads the infection with probability 1.0. */
public final class InfectedSymptomaticState extends InfectedState {
    public static final InfectedSymptomaticState INSTANCE = new InfectedSymptomaticState();

    private InfectedSymptomaticState() {
    }

    @Override
    public HealthKind kind() {
        return HealthKind.INFECTED_SYMPTOMATIC;
    }

    @Override
    public double transmissionProbability() {
        return SimulationConfig.TRANSMISSION_SYMPTOMATIC;
    }
}
