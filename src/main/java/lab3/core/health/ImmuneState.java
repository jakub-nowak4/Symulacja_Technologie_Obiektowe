package lab3.core.health;

/** Immune to the infection: either from the start or after recovering. Final state. */
public final class ImmuneState implements HealthState {
    public static final ImmuneState INSTANCE = new ImmuneState();

    private ImmuneState() {
    }

    @Override
    public HealthKind kind() {
        return HealthKind.IMMUNE;
    }

    @Override
    public boolean isSusceptible() {
        return false;
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
        return this;
    }

    @Override
    public HealthState afterTick(long infectionTicksLeft) {
        return this;
    }
}
