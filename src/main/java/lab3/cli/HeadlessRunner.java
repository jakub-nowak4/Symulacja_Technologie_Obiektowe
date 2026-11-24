package lab3.cli;

import lab3.core.Simulation;
import lab3.core.SimulationConfig;
import lab3.core.SimulationEngine;
import lab3.core.Ticks;
import lab3.core.view.Stats;
import lab3.core.view.WorldView;

import java.io.PrintStream;

/**
 * Runs a whole simulation without any window and prints one CSV row per simulated second.
 * It talks to the simulation only through {@link SimulationEngine}, which shows that the core
 * works without a user interface.
 *
 * <pre>
 * java -cp target/classes lab3.cli.HeadlessRunner --seconds 600 --scenario 1 --seed 42
 * </pre>
 */
public final class HeadlessRunner {
    static final String HEADER = "t,N,healthy,infAsym,infSym,immune";

    /** Share of immune people in scenario 2 unless {@code --immune} says otherwise. */
    static final double SCENARIO_2_IMMUNITY = 0.3;

    static final String USAGE = """
            Usage: HeadlessRunner [--seconds T] [--scenario 1|2] [--seed S] [--immune P]
              --seconds T   simulated time in whole seconds, at least 1 (default 60)
              --scenario    1 = nobody is immune, 2 = part of the people is immune (default 1)
              --seed S      seed of the random number generator (default 42)
              --immune P    share of immune people in [0, 1]; replaces the scenario's value
              --help        print this text
            Output: CSV with a row for every simulated second, starting with t = 0.
            """;

    /** Validated command line options. */
    record Options(int seconds, int scenario, long seed, double immune) {
        static final int DEFAULT_SECONDS = 60;
        static final long DEFAULT_SEED = 42;

        Options {
            if (seconds < 1) {
                throw new IllegalArgumentException("--seconds must be at least 1: " + seconds);
            }
            if (scenario != 1 && scenario != 2) {
                throw new IllegalArgumentException("--scenario must be 1 or 2: " + scenario);
            }
            if (!(immune >= 0 && immune <= 1)) {
                throw new IllegalArgumentException("--immune must be in [0, 1]: " + immune);
            }
        }

        /** Parses the arguments; a missing option gets its default. */
        static Options parse(String[] args) {
            int seconds = DEFAULT_SECONDS;
            int scenario = 1;
            long seed = DEFAULT_SEED;
            Double immune = null;
            for (int i = 0; i < args.length; i += 2) {
                String name = args[i];
                if (i + 1 >= args.length) {
                    throw new IllegalArgumentException("missing value for " + name);
                }
                String value = args[i + 1];
                try {
                    switch (name) {
                        case "--seconds" -> seconds = Integer.parseInt(value);
                        case "--scenario" -> scenario = Integer.parseInt(value);
                        case "--seed" -> seed = Long.parseLong(value);
                        case "--immune" -> immune = Double.parseDouble(value);
                        default -> throw new IllegalArgumentException("unknown option " + name);
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("invalid value for " + name + ": " + value);
                }
            }
            double share = immune != null ? immune : (scenario == 2 ? SCENARIO_2_IMMUNITY : 0.0);
            return new Options(seconds, scenario, seed, share);
        }

        SimulationConfig config() {
            return SimulationConfig.withImmunity(immune);
        }
    }

    private HeadlessRunner() {
    }

    public static void main(String[] args) {
        int exitCode = run(args, System.out, System.err);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    /**
     * Runs the program.
     *
     * @return 0 on success, 2 when the arguments are wrong (the reason goes to {@code err})
     */
    static int run(String[] args, PrintStream out, PrintStream err) {
        for (String arg : args) {
            if (arg.equals("--help")) {
                out.print(USAGE);
                return 0;
            }
        }
        Options options;
        try {
            options = Options.parse(args);
        } catch (IllegalArgumentException e) {
            err.println("error: " + e.getMessage());
            err.print(USAGE);
            return 2;
        }
        writeCsv(Simulation.withSeed(options.config(), options.seed()), options.seconds(), out);
        return 0;
    }

    /** Prints the header and a row for t = 0, 1, ..., {@code seconds}, advancing the engine as needed. */
    static void writeCsv(SimulationEngine engine, int seconds, PrintStream out) {
        out.print(HEADER + "\n");
        printRow(engine.view(), out);
        for (int second = 1; second <= seconds; second++) {
            for (int tick = 0; tick < Ticks.PER_SECOND; tick++) {
                engine.step();
            }
            printRow(engine.view(), out);
        }
    }

    private static void printRow(WorldView view, PrintStream out) {
        Stats stats = view.stats();
        out.print((long) view.seconds() + "," + stats.total() + "," + stats.healthy() + ","
                + stats.infectedAsymptomatic() + "," + stats.infectedSymptomatic() + "," + stats.immune() + "\n");
    }
}
