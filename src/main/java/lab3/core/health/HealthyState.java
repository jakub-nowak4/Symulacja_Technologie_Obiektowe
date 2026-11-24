package lab3.core.health;

/** Susceptible and not infected. */
public final class HealthyState implements HealthState {
    public static final HealthyState INSTANCE = new HealthyState();

    private HealthyState() {
    }

    @Override
    public HealthKind kind() {
        return HealthKind.HEALTHY;
    }

    @Override
    public boolean isSusceptible() {
        return true;
    }

    @Override
    public boolean isInfectious() {
        return false;
    }

    @Override
    public double transmissionProbability() {
        return 0.0;
    }

    @Override
    public HealthState infect(boolean symptomatic) {
        return symptomatic ? InfectedSymptomaticState.INSTANCE : InfectedAsymptomaticState.INSTANCE;
    }

    @Override
    public HealthState afterTick(long infectionTicksLeft) {
        return this;
    }
}
