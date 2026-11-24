package lab3.core;

import lab3.core.view.PersonView;
import lab3.core.view.WorldView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The simulation: owns the people, the random number generator, the contact counters and the time.
 *
 * <p>One {@link #step()} is one tick and always runs in the same order (see docs/PLAN.md, section 5):
 * <ol>
 *   <li>every person moves, in order of ids, and the border decides whether they bounce or leave;</li>
 *   <li>the illness of everybody who is still here counts one tick down;</li>
 *   <li>a new person may enter through the border;</li>
 *   <li>contacts are evaluated on the states as they are at this point (assumption A12);</li>
 *   <li>the resulting infections are applied;</li>
 *   <li>time advances by one tick.</li>
 * </ol>
 * The countdown comes before entering and infecting, so that a new infection lasts for its full
 * drawn length. The list of people is always sorted by id, so the run depends on the seed only.
 */
public final class Simulation implements SimulationEngine {
    private final SimulationConfig config;
    private final Rng rng;
    private final BoundaryPolicy boundary;
    private final EntryPolicy entry;
    private final InfectionPolicy infectionPolicy;
    private final ContactTracker contacts;

    private final List<Person> persons;
    private long tick;
    private int nextPersonId;

    /** Starts a new simulation: creates the initial population from {@code rng}. */
    public Simulation(SimulationConfig config, Rng rng) {
        this(config, rng, new ArrayList<>(), 0, 0);
        persons.addAll(entry.createInitialPopulation(rng, this::takeId));
    }

    /** Convenience for runners and tests: a simulation driven by a {@link SplitMix64Rng}. */
    public static Simulation withSeed(SimulationConfig config, long seed) {
        return new Simulation(config, new SplitMix64Rng(seed));
    }

    /**
     * Builds a simulation in a given situation. Used by tests now and, later, to restore a memento.
     *
     * @param persons      people sorted by ascending id; the list is taken over
     * @param tick         ticks elapsed so far
     * @param nextPersonId id the next entering person gets; greater than every id in {@code persons}
     */
    Simulation(SimulationConfig config, Rng rng, List<Person> persons, long tick, int nextPersonId) {
        if (config == null || rng == null || persons == null) {
            throw new IllegalArgumentException("config, rng and persons must not be null");
        }
        if (tick < 0) {
            throw new IllegalArgumentException("tick must not be negative: " + tick);
        }
        int previousId = -1;
        for (Person person : persons) {
            if (person.id() <= previousId) {
                throw new IllegalArgumentException("persons must have strictly ascending ids");
            }
            previousId = person.id();
        }
        if (nextPersonId <= previousId) {
            throw new IllegalArgumentException("nextPersonId must be greater than every person id");
        }
        this.config = config;
        this.rng = rng;
        this.boundary = new BoundaryPolicy(new Area(config.areaWidth(), config.areaHeight()));
        this.entry = new EntryPolicy(config);
        this.infectionPolicy = new InfectionPolicy(config);
        this.contacts = new ContactTracker();
        this.persons = persons;
        this.tick = tick;
        this.nextPersonId = nextPersonId;
    }

    @Override
    public void step() {
        moveAndCheckBorder();
        for (Person person : persons) {
            person.advanceHealth();
        }
        entry.maybeEnter(rng, this::takeId).ifPresent(persons::add);
        applyInfections(contacts.evaluate(persons, rng));
        tick++;
    }

    @Override
    public WorldView view() {
        List<PersonView> views = new ArrayList<>(persons.size());
        for (Person person : persons) {
            views.add(person.view());
        }
        return new WorldView(tick, config.areaWidth(), config.areaHeight(), views);
    }

    private void moveAndCheckBorder() {
        Iterator<Person> it = persons.iterator();
        while (it.hasNext()) {
            Person person = it.next();
            Motion moved = RandomWalk.next(person.motion(), rng, config);
            BoundaryResult result = boundary.apply(moved, rng);
            if (result.outcome() == BoundaryResult.Outcome.EXITED) {
                it.remove();
            } else {
                person.setMotion(result.motion());
            }
        }
    }

    private void applyInfections(List<Infection> infections) {
        if (infections.isEmpty()) {
            return;
        }
        Map<Integer, Person> byId = new HashMap<>();
        for (Person person : persons) {
            byId.put(person.id(), person);
        }
        for (Infection infection : infections) {
            infectionPolicy.infect(byId.get(infection.targetId()), rng);
        }
    }

    private int takeId() {
        return nextPersonId++;
    }
}
