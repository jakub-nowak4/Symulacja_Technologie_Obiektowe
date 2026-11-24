package lab3.cli;

import lab3.cli.HeadlessRunner.Options;
import lab3.core.Simulation;
import lab3.core.SimulationConfig;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeadlessRunnerTest {

    private static final class Result {
        int exitCode;
        String out;
        String err;
    }

    private static Result run(String... args) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        Result result = new Result();
        result.exitCode = HeadlessRunner.run(args,
                new PrintStream(out, true, StandardCharsets.UTF_8), new PrintStream(err, true, StandardCharsets.UTF_8));
        result.out = out.toString(StandardCharsets.UTF_8);
        result.err = err.toString(StandardCharsets.UTF_8);
        return result;
    }

    // ---- options

    @Test
    void defaultsAreSixtySecondsScenarioOneSeedFortyTwo() {
        Options options = Options.parse(new String[0]);

        assertEquals(60, options.seconds());
        assertEquals(1, options.scenario());
        assertEquals(42, options.seed());
        assertEquals(0.0, options.immune(), 0);
    }

    @Test
    void parsesAllOptions() {
        Options options = Options.parse(new String[] {"--seconds", "600", "--scenario", "2", "--seed", "7", "--immune", "0.5"});

        assertEquals(600, options.seconds());
        assertEquals(2, options.scenario());
        assertEquals(7, options.seed());
        assertEquals(0.5, options.immune(), 0);
    }

    @Test
    void scenarioTwoUsesTheDefaultImmunityUnlessOverridden() {
        assertEquals(HeadlessRunner.SCENARIO_2_IMMUNITY, Options.parse(new String[] {"--scenario", "2"}).immune(), 0);
        assertEquals(0.8, Options.parse(new String[] {"--scenario", "2", "--immune", "0.8"}).immune(), 0);
        assertEquals(0.0, Options.parse(new String[] {"--scenario", "1"}).immune(), 0);
    }

    @Test
    void optionsMayComeInAnyOrder() {
        Options options = Options.parse(new String[] {"--seed", "5", "--seconds", "10"});

        assertEquals(5, options.seed());
        assertEquals(10, options.seconds());
    }

    @Test
    void rejectsBadArguments() {
        assertThrows(IllegalArgumentException.class, () -> Options.parse(new String[] {"--seconds"}));
        assertThrows(IllegalArgumentException.class, () -> Options.parse(new String[] {"--seconds", "abc"}));
        assertThrows(IllegalArgumentException.class, () -> Options.parse(new String[] {"--seconds", "0"}));
        assertThrows(IllegalArgumentException.class, () -> Options.parse(new String[] {"--seconds", "-5"}));
        assertThrows(IllegalArgumentException.class, () -> Options.parse(new String[] {"--scenario", "3"}));
        assertThrows(IllegalArgumentException.class, () -> Options.parse(new String[] {"--immune", "1.5"}));
        assertThrows(IllegalArgumentException.class, () -> Options.parse(new String[] {"--immune", "NaN"}));
        assertThrows(IllegalArgumentException.class, () -> Options.parse(new String[] {"--speed", "2"}));
        assertThrows(IllegalArgumentException.class, () -> Options.parse(new String[] {"10"}));
    }

    // ---- output

    @Test
    void printsTheHeaderAndOneRowPerSecondIncludingZero() {
        Result result = run("--seconds", "5", "--seed", "1");

        String[] lines = result.out.split("\n");
        assertEquals(0, result.exitCode);
        assertEquals(7, lines.length);
        assertEquals("t,N,healthy,infAsym,infSym,immune", lines[0]);
        for (int t = 0; t <= 5; t++) {
            assertTrue(lines[t + 1].startsWith(t + ","), lines[t + 1]);
        }
        assertEquals("", result.err);
    }

    @Test
    void firstRowIsTheInitialPopulation() {
        String[] lines = run("--seconds", "1", "--scenario", "1").out.split("\n");

        assertEquals("0,60,60,0,0,0", lines[1]);
    }

    @Test
    void scenarioTwoStartsWithSomeImmunePeople() {
        String[] first = run("--seconds", "1", "--scenario", "2", "--seed", "3").out.split("\n")[1].split(",");

        assertEquals("60", first[1]);
        int immune = Integer.parseInt(first[5]);
        assertTrue(immune > 0 && immune < 60, "immune " + immune);
    }

    @Test
    void everyRowAddsUp() {
        String[] lines = run("--seconds", "120", "--seed", "4").out.split("\n");

        for (int i = 1; i < lines.length; i++) {
            String[] c = lines[i].split(",");
            int sum = Integer.parseInt(c[2]) + Integer.parseInt(c[3]) + Integer.parseInt(c[4]) + Integer.parseInt(c[5]);
            assertEquals(Integer.parseInt(c[1]), sum, lines[i]);
        }
    }

    @Test
    void sameSeedGivesTheSameOutputAndDifferentSeedsDiffer() {
        String a = run("--seconds", "60", "--seed", "9").out;
        String b = run("--seconds", "60", "--seed", "9").out;
        String c = run("--seconds", "60", "--seed", "10").out;

        assertEquals(a, b);
        assertFalse(a.equals(c));
    }

    @Test
    void usesLineFeedOnlyWhateverThePlatform() {
        assertFalse(run("--seconds", "2").out.contains("\r"));
    }

    @Test
    void everyRowMatchesTheEngineStatsAfterTheSameNumberOfSteps() {
        String[] lines = run("--seconds", "120", "--seed", "11").out.split("\n");
        Simulation simulation = Simulation.withSeed(SimulationConfig.noImmunity(), 11);
        boolean sawInfected = false;

        for (int second = 0; second <= 120; second++) {
            if (second > 0) {
                for (int i = 0; i < 25; i++) {
                    simulation.step();
                }
            }
            var stats = simulation.view().stats();
            String expected = second + "," + stats.total() + "," + stats.healthy() + "," + stats.infectedAsymptomatic()
                    + "," + stats.infectedSymptomatic() + "," + stats.immune();
            assertEquals(expected, lines[second + 1]);
            sawInfected |= stats.infectedAsymptomatic() != stats.infectedSymptomatic();
        }
        assertTrue(sawInfected, "the run should contain infected people, or the columns are not really checked");
    }

    // ---- errors and help

    @Test
    void wrongArgumentsGiveExitCodeTwoAndNothingOnStandardOutput() {
        Result result = run("--scenario", "7");

        assertEquals(2, result.exitCode);
        assertEquals("", result.out);
        assertTrue(result.err.startsWith("error: --scenario must be 1 or 2"), result.err);
        assertTrue(result.err.contains("Usage:"));
    }

    @Test
    void helpPrintsTheUsageAndSucceeds() {
        Result result = run("--help");

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("Usage:"));
        assertEquals("", result.err);
    }
}
