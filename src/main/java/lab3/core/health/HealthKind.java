package lab3.core.health;

/** Health category of a person, as seen from outside the simulation (view, statistics, memento). */
public enum HealthKind {
    HEALTHY,
    IMMUNE,
    INFECTED_ASYMPTOMATIC,
    INFECTED_SYMPTOMATIC;

    public boolean isInfected() {
        return this == INFECTED_ASYMPTOMATIC || this == INFECTED_SYMPTOMATIC;
    }
}
