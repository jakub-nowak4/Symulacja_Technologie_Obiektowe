package lab3.io;

import lab3.core.SimulationConfig;
import lab3.core.health.HealthKind;
import lab3.core.memento.ContactSnapshot;
import lab3.core.memento.PersonSnapshot;
import lab3.core.memento.SimulationMemento;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Saves a {@link SimulationMemento} to a file and reads it back.
 *
 * <p>The file is plain UTF-8 text, one fact per line, so that it can be read (and checked) in any
 * editor and so that reading it can never run foreign code, as Java serialization could. The first
 * line names the format and its version. Numbers are written with {@link Double#toString(double)}
 * and read with {@link Double#parseDouble(String)}, which gives back exactly the same value, so a
 * restored simulation continues exactly as the saved one would have.
 *
 * <pre>
 * lab3-memento 1
 * areaWidth 30.0                      (the eleven configuration values, one per line)
 * ...
 * tick 250
 * rngState -3523870230921562327
 * nextPersonId 64
 * persons 2
 * person 0 10.5 20.25 -1.0 0.5 HEALTHY 0     (id x y vx vy kind infectionTicksLeft)
 * person 3 4.0 5.0 0.0 1.5 INFECTED_SYMPTOMATIC 612
 * contacts 1
 * contact 0 3 17 false                       (target source ticks drawn)
 * end
 * </pre>
 *
 * <p>A file that does not follow this exactly is rejected with a {@link SnapshotFormatException}
 * that says which line is wrong; nothing else is thrown for bad content, and a file larger than
 * {@link #MAX_BYTES} is not read at all.
 */
public final class SnapshotStore {
    static final String FORMAT = "lab3-memento";
    static final int VERSION = 1;

    /** Largest file that is read; a memento of a few hundred people is far smaller. */
    public static final long MAX_BYTES = 16L * 1024 * 1024;

    private SnapshotStore() {
    }

    // ---- files

    /** Writes the memento to the file, replacing it if it exists. */
    public static void save(Path file, SimulationMemento memento) throws IOException {
        Files.writeString(file, toText(memento), StandardCharsets.UTF_8);
    }

    /**
     * Reads a memento from the file.
     *
     * @throws SnapshotFormatException if the content is not a valid memento
     * @throws IOException             if the file cannot be read
     */
    public static SimulationMemento load(Path file) throws IOException {
        long size = Files.size(file);
        if (size > MAX_BYTES) {
            throw new SnapshotFormatException("file is too large for a memento: " + size + " bytes");
        }
        byte[] bytes = Files.readAllBytes(file);
        String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            throw new SnapshotFormatException("file is not valid UTF-8 text", e);
        }
        return fromText(text);
    }

    // ---- text

    /** Writes a memento in the file format; lines end with a line feed. */
    public static String toText(SimulationMemento memento) {
        SimulationConfig c = memento.config();
        StringBuilder out = new StringBuilder();
        line(out, FORMAT + " " + VERSION);
        line(out, "areaWidth " + c.areaWidth());
        line(out, "areaHeight " + c.areaHeight());
        line(out, "initialPopulation " + c.initialPopulation());
        line(out, "initialInfected " + c.initialInfected());
        line(out, "entryRatePerSecond " + c.entryRatePerSecond());
        line(out, "minSpeed " + c.minSpeed());
        line(out, "immuneProbability " + c.immuneProbability());
        line(out, "symptomaticProbability " + c.symptomaticProbability());
        line(out, "velocityChangeProbability " + c.velocityChangeProbability());
        line(out, "maxTurnAngle " + c.maxTurnAngle());
        line(out, "maxSpeedDelta " + c.maxSpeedDelta());
        line(out, "tick " + memento.tick());
        line(out, "rngState " + memento.rngState());
        line(out, "nextPersonId " + memento.nextPersonId());
        line(out, "persons " + memento.persons().size());
        for (PersonSnapshot p : memento.persons()) {
            line(out, "person " + p.id() + " " + p.x() + " " + p.y() + " " + p.vx() + " " + p.vy()
                    + " " + p.kind() + " " + p.infectionTicksLeft());
        }
        line(out, "contacts " + memento.contacts().size());
        for (ContactSnapshot k : memento.contacts()) {
            line(out, "contact " + k.targetId() + " " + k.sourceId() + " " + k.ticks() + " " + k.drawn());
        }
        line(out, "end");
        return out.toString();
    }

    /**
     * Reads a memento written by {@link #toText}. Both {@code \n} and {@code \r\n} are accepted.
     *
     * @throws SnapshotFormatException if the text is not a valid memento
     */
    public static SimulationMemento fromText(String text) throws SnapshotFormatException {
        Reader reader = new Reader(text);
        try {
            return reader.read();
        } catch (SnapshotFormatException e) {
            throw e;
        } catch (RuntimeException e) {
            // the records validate their contents; whatever they reject is a damaged file
            throw reader.error(e.getMessage(), e);
        }
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    /** Reads the lines one by one and reports the number of the line that is wrong. */
    private static final class Reader {
        private final List<String> lines;
        private int index;   // number of lines read so far

        Reader(String text) {
            this.lines = text.lines().toList();
        }

        SimulationMemento read() throws SnapshotFormatException {
            String[] header = tokens("header", 2);
            if (!header[0].equals(FORMAT)) {
                throw error("not a memento file (expected \"" + FORMAT + "\" but found \"" + header[0] + "\")", null);
            }
            if (!header[1].equals(Integer.toString(VERSION))) {
                throw error("unsupported format version " + header[1] + " (this program reads version " + VERSION + ")", null);
            }

            double areaWidth = decimal("areaWidth");
            double areaHeight = decimal("areaHeight");
            int initialPopulation = integer("initialPopulation");
            int initialInfected = integer("initialInfected");
            double entryRate = decimal("entryRatePerSecond");
            double minSpeed = decimal("minSpeed");
            double immune = decimal("immuneProbability");
            double symptomatic = decimal("symptomaticProbability");
            double velocityChange = decimal("velocityChangeProbability");
            double maxTurn = decimal("maxTurnAngle");
            double maxSpeedDelta = decimal("maxSpeedDelta");
            SimulationConfig config;
            try {
                config = new SimulationConfig(areaWidth, areaHeight, initialPopulation, initialInfected, entryRate,
                        minSpeed, immune, symptomatic, velocityChange, maxTurn, maxSpeedDelta);
            } catch (IllegalArgumentException e) {
                throw new SnapshotFormatException("lines 2-12: invalid configuration: " + e.getMessage(), e);
            }
            long tick = whole("tick");
            long rngState = whole("rngState");
            int nextPersonId = integer("nextPersonId");

            int personCount = count("persons");
            List<PersonSnapshot> persons = new ArrayList<>();
            for (int i = 0; i < personCount; i++) {
                String[] t = tokens("person", 8);
                expectWord(t[0], "person");
                persons.add(new PersonSnapshot(toInt(t[1]), toDouble(t[2]), toDouble(t[3]), toDouble(t[4]), toDouble(t[5]),
                        kind(t[6]), toLong(t[7])));
            }

            int contactCount = count("contacts");
            List<ContactSnapshot> contacts = new ArrayList<>();
            for (int i = 0; i < contactCount; i++) {
                String[] t = tokens("contact", 5);
                expectWord(t[0], "contact");
                contacts.add(new ContactSnapshot(toInt(t[1]), toInt(t[2]), toLong(t[3]), toBoolean(t[4])));
            }

            String[] end = tokens("end", 1);
            expectWord(end[0], "end");
            for (int i = index; i < lines.size(); i++) {
                if (!lines.get(i).isBlank()) {
                    index = i + 1;
                    throw error("unexpected text after \"end\"", null);
                }
            }
            try {
                return new SimulationMemento(config, tick, rngState, nextPersonId, persons, contacts);
            } catch (IllegalArgumentException e) {
                throw new SnapshotFormatException("inconsistent content: " + e.getMessage(), e);
            }
        }

        // ---- lines

        private String[] tokens(String what, int expected) throws SnapshotFormatException {
            if (index >= lines.size()) {
                throw error("file ends too early, \"" + what + "\" is missing", null);
            }
            String line = lines.get(index++);
            String[] tokens = line.split(" ", -1);
            if (tokens.length != expected) {
                throw error("expected " + expected + " items in \"" + what + "\" line but found " + tokens.length, null);
            }
            for (String token : tokens) {
                if (token.isEmpty()) {
                    throw error("empty item in \"" + what + "\" line", null);
                }
            }
            return tokens;
        }

        /** Reads a line "key value" and returns the value. */
        private String value(String key) throws SnapshotFormatException {
            String[] t = tokens(key, 2);
            expectWord(t[0], key);
            return t[1];
        }

        private double decimal(String key) throws SnapshotFormatException {
            return toDouble(value(key));
        }

        private int integer(String key) throws SnapshotFormatException {
            return toInt(value(key));
        }

        private long whole(String key) throws SnapshotFormatException {
            return toLong(value(key));
        }

        private int count(String key) throws SnapshotFormatException {
            int count = integer(key);
            // every item takes a line, so a count larger than the remaining lines cannot be right
            if (count < 0 || count > lines.size() - index) {
                throw error("\"" + key + "\" says " + count + " but the file has only " + (lines.size() - index)
                        + " lines left", null);
            }
            return count;
        }

        // ---- items

        private void expectWord(String found, String expected) throws SnapshotFormatException {
            if (!found.equals(expected)) {
                throw error("expected \"" + expected + "\" but found \"" + found + "\"", null);
            }
        }

        private double toDouble(String s) throws SnapshotFormatException {
            try {
                double value = Double.parseDouble(s);
                if (!Double.isFinite(value)) {
                    throw error("number is not finite: " + s, null);
                }
                return value;
            } catch (NumberFormatException e) {
                throw error("not a number: " + s, e);
            }
        }

        private int toInt(String s) throws SnapshotFormatException {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                throw error("not a whole number: " + s, e);
            }
        }

        private long toLong(String s) throws SnapshotFormatException {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException e) {
                throw error("not a whole number: " + s, e);
            }
        }

        private boolean toBoolean(String s) throws SnapshotFormatException {
            return switch (s) {
                case "true" -> true;
                case "false" -> false;
                default -> throw error("expected true or false but found \"" + s + "\"", null);
            };
        }

        private HealthKind kind(String s) throws SnapshotFormatException {
            try {
                return HealthKind.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw error("unknown health kind: " + s, e);
            }
        }

        SnapshotFormatException error(String message, Throwable cause) {
            String where = index == 0 ? "line 1" : "line " + index;
            return new SnapshotFormatException(where + ": " + message, cause);
        }
    }
}
