package lab3.core;

import lab3.core.health.HealthKind;
import lab3.core.health.ImmuneState;
import lab3.vectors.Vector2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.IntSupplier;

/**
 * Creates people: the initial population and, tick by tick, people entering through the border
 * (assumptions A4-A7 in docs/PLAN.md).
 *
 * <p>Health of a new person: first the immunity draw ({@code immuneProbability}); only a person who
 * is not immune can be infected (10% for entering people). Ids come from the caller, so that the
 * simulation owns the id sequence.
 */
public final class EntryPolicy {
    private final SimulationConfig config;
    private final Area area;
    private final InfectionPolicy infectionPolicy;

    public EntryPolicy(SimulationConfig config) {
        this.config = config;
        this.area = new Area(config.areaWidth(), config.areaHeight());
        this.infectionPolicy = new InfectionPolicy(config);
    }

    /**
     * Creates the initial population at random positions with random velocities. The first
     * {@code initialInfected} people are infected, the others are immune with probability
     * {@code immuneProbability} and healthy otherwise.
     */
    public List<Person> createInitialPopulation(Rng rng, IntSupplier ids) {
        List<Person> people = new ArrayList<>(config.initialPopulation());
        for (int i = 0; i < config.initialPopulation(); i++) {
            Vector2D position = area.randomInteriorPoint(rng);
            Motion motion = new Motion(position, randomVelocity(rng));
            if (i < config.initialInfected()) {
                Person person = Person.healthy(ids.getAsInt(), motion);
                infectionPolicy.infect(person, rng);
                people.add(person);
            } else {
                people.add(personWithImmunityDraw(ids.getAsInt(), motion, rng));
            }
        }
        return people;
    }

    /**
     * Decides whether a person enters in this tick (one draw, probability
     * {@link SimulationConfig#entryProbabilityPerTick()}) and, if so, creates them on the border,
     * moving inwards. The id supplier is called only when someone enters.
     */
    public Optional<Person> maybeEnter(Rng rng, IntSupplier ids) {
        if (!rng.chance(config.entryProbabilityPerTick())) {
            return Optional.empty();
        }
        Vector2D position = area.randomBoundaryPoint(rng);
        Vector2D velocity = area.directInward(position, randomVelocity(rng));
        Person person = personWithImmunityDraw(ids.getAsInt(), new Motion(position, velocity), rng);
        if (person.kind() == HealthKind.HEALTHY && rng.chance(SimulationConfig.ENTRY_INFECTED_PROBABILITY)) {
            infectionPolicy.infect(person, rng);
        }
        return Optional.of(person);
    }

    /** Speed uniform in {@code [minSpeed, 2.5)}, direction uniform in all directions (two draws). */
    private Vector2D randomVelocity(Rng rng) {
        double speed = rng.range(config.minSpeed(), SimulationConfig.MAX_SPEED);
        double heading = rng.range(0, 2 * Math.PI);
        return Vector2D.fromPolar(speed, heading);
    }

    private Person personWithImmunityDraw(int id, Motion motion, Rng rng) {
        if (rng.chance(config.immuneProbability())) {
            return new Person(id, motion, ImmuneState.INSTANCE);
        }
        return Person.healthy(id, motion);
    }
}
