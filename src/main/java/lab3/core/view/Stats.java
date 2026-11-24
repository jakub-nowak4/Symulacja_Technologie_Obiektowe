package lab3.core.view;

import java.util.List;

/** Number of people in each health category. */
public record Stats(int healthy, int infectedAsymptomatic, int infectedSymptomatic, int immune) {

    public Stats {
        if (healthy < 0 || infectedAsymptomatic < 0 || infectedSymptomatic < 0 || immune < 0) {
            throw new IllegalArgumentException("counts must not be negative");
        }
    }

    public static Stats of(List<PersonView> persons) {
        int healthy = 0;
        int asymptomatic = 0;
        int symptomatic = 0;
        int immune = 0;
        for (PersonView person : persons) {
            switch (person.kind()) {
                case HEALTHY -> healthy++;
                case INFECTED_ASYMPTOMATIC -> asymptomatic++;
                case INFECTED_SYMPTOMATIC -> symptomatic++;
                case IMMUNE -> immune++;
            }
        }
        return new Stats(healthy, asymptomatic, symptomatic, immune);
    }

    public int infected() {
        return infectedAsymptomatic + infectedSymptomatic;
    }

    public int total() {
        return healthy + infected() + immune;
    }
}
