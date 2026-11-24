package lab3.core.health;

/** Behaviour shared by both infected states: cannot be infected again, recovers into immunity. */
abstract class InfectedState implements HealthState {

    @Override
    public final boolean isSusceptible() {
        return false;
    }

    @Override
    public final boolean isInfectious() {
        return true;
    }

    @Override
    public final HealthState infect(boolean symptomatic) {
        return this;
    }

    @Override
    public final HealthState afterTick(long infectionTicksLeft) {
        return infectionTicksLeft <= 0 ? ImmuneState.INSTANCE : this;
    }
}
