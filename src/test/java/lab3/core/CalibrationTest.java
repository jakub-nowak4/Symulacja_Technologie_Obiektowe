package lab3.core;

import lab3.core.view.Stats;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the criteria of docs/PLAN.md for the calibrated parameters (stage 17): the population stays
 * around its initial size, the infection spreads and keeps going in scenario 1, and immunity makes
 * it weaker in scenario 2. Several seeds are used and the limits are deliberately wide, because
 * only the overall behaviour matters, not one particular run.
 */
class CalibrationTest {
    private static final int SECONDS = 600;
    private static final long[] SEEDS = {1, 2, 3, 4, 5, 6};
    private static final double SCENARIO_2_IMMUNITY = 0.3;

    /** Statistics at t = 0, 1, ..., SECONDS. */
    private static List<Stats> run(SimulationConfig config, long seed) {
        Simulation simulation = Simulation.withSeed(config, seed);
        List<Stats> series = new ArrayList<>();
        series.add(simulation.view().stats());
        for (int second = 1; second <= SECONDS; second++) {
            for (int tick = 0; tick < Ticks.PER_SECOND; tick++) {
                simulation.step();
            }
            series.add(simulation.view().stats());
        }
        return series;
    }

    private static double meanInfected(List<Stats> series) {
        return series.stream().mapToInt(Stats::infected).average().orElseThrow();
    }

    @Test
    void populationStaysAroundTheInitialSize() {
        SimulationConfig config = SimulationConfig.noImmunity();
        int initial = config.initialPopulation();
        double sum = 0;

        for (long seed : SEEDS) {
            List<Stats> series = run(config, seed);
            for (Stats stats : series) {
                assertTrue(stats.total() >= 0.4 * initial && stats.total() <= 1.6 * initial,
                        "seed " + seed + ": population " + stats.total());
            }
            sum += series.stream().mapToInt(Stats::total).average().orElseThrow();
        }

        double mean = sum / SEEDS.length;
        assertTrue(mean >= 0.8 * initial && mean <= 1.2 * initial, "mean population " + mean);
    }

    /** The infection may be absent for a while (then an infected entrant restarts it), but not for long. */
    @Test
    void infectionStartsSpreadsAndDoesNotDieOutInScenarioOne() {
        for (long seed : SEEDS) {
            List<Stats> series = run(SimulationConfig.noImmunity(), seed);

            int first = -1;
            int peak = 0;
            int withInfected = 0;
            for (int t = 0; t < series.size(); t++) {
                int infected = series.get(t).infected();
                if (infected > 0 && first < 0) {
                    first = t;
                }
                peak = Math.max(peak, infected);
                if (t >= 60 && infected > 0) {
                    withInfected++;
                }
            }

            assertTrue(first >= 0 && first <= 60, "seed " + seed + ": first infection at " + first + " s");
            assertTrue(peak >= 8, "seed " + seed + ": peak of infected people " + peak);
            assertTrue(withInfected >= 0.85 * (SECONDS - 60 + 1),
                    "seed " + seed + ": infection present in only " + withInfected + " seconds");
        }
    }

    @Test
    void immunityMakesTheInfectionWeaker() {
        double scenarioOne = 0;
        double scenarioTwo = 0;

        for (long seed : SEEDS) {
            scenarioOne += meanInfected(run(SimulationConfig.noImmunity(), seed));
            scenarioTwo += meanInfected(run(SimulationConfig.withImmunity(SCENARIO_2_IMMUNITY), seed));
        }

        assertTrue(scenarioTwo < 0.85 * scenarioOne,
                "mean infected: scenario 1 " + scenarioOne / SEEDS.length + ", scenario 2 " + scenarioTwo / SEEDS.length);
    }

    @Test
    void scenarioTwoStartsWithImmunePeopleAndKeepsThemAround() {
        for (long seed : SEEDS) {
            List<Stats> series = run(SimulationConfig.withImmunity(SCENARIO_2_IMMUNITY), seed);
            double meanImmune = series.stream().mapToInt(Stats::immune).average().orElseThrow();
            double meanTotal = series.stream().mapToInt(Stats::total).average().orElseThrow();

            assertTrue(series.get(0).immune() > 0, "seed " + seed + ": nobody immune at the start");
            assertTrue(meanImmune / meanTotal > 0.15, "seed " + seed + ": immune share " + meanImmune / meanTotal);
        }
    }
}
