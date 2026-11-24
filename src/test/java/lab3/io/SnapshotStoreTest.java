package lab3.io;

import lab3.core.Simulation;
import lab3.core.SimulationConfig;
import lab3.core.memento.ContactSnapshot;
import lab3.core.memento.PersonSnapshot;
import lab3.core.memento.SimulationMemento;
import lab3.core.health.HealthKind;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnapshotStoreTest {

    private static final long THRESHOLD = SimulationConfig.CONTACT_TICKS;

    /** A small memento with every kind of content: people of all kinds and contacts of both kinds. */
    private static SimulationMemento small() {
        return new SimulationMemento(SimulationConfig.noImmunity(), 250, -3523870230921562327L, 64,
                List.of(
                        new PersonSnapshot(0, 10.5, 20.25, -1.0, 0.5, HealthKind.HEALTHY, 0),
                        new PersonSnapshot(3, 4.0, 5.0, 0.0, 1.5, HealthKind.INFECTED_SYMPTOMATIC, 612),
                        new PersonSnapshot(9, 4.5, 5.0, 0.25, -0.75, HealthKind.IMMUNE, 0),
                        new PersonSnapshot(12, 1.0, 1.0, 1.0, 1.0, HealthKind.INFECTED_ASYMPTOMATIC, 501)),
                List.of(new ContactSnapshot(0, 3, 17, false), new ContactSnapshot(0, 12, THRESHOLD + 4, true)));
    }

    private static SimulationMemento busy(long seed, int ticks) {
        Simulation simulation = Simulation.withSeed(SimulationConfig.noImmunity(), seed);
        for (int i = 0; i < ticks; i++) {
            simulation.step();
        }
        return simulation.createMemento();
    }

    private static SnapshotFormatException rejected(String text) {
        return assertThrows(SnapshotFormatException.class, () -> SnapshotStore.fromText(text));
    }

    private static String replaceLine(String text, int line, String replacement) {
        List<String> lines = new ArrayList<>(text.lines().toList());
        lines.set(line - 1, replacement);
        return String.join("\n", lines) + "\n";
    }

    private static String removeLine(String text, int line) {
        List<String> lines = new ArrayList<>(text.lines().toList());
        lines.remove(line - 1);
        return String.join("\n", lines) + "\n";
    }

    private interface FileAction {
        void run(Path directory) throws IOException;
    }

    private static void inTempDirectory(FileAction action) throws IOException {
        Path directory = Files.createTempDirectory("lab3-snapshot-test");
        try {
            action.run(directory);
        } finally {
            try (Stream<Path> files = Files.walk(directory)) {
                for (Path p : files.sorted((a, b) -> b.compareTo(a)).toList()) {
                    Files.deleteIfExists(p);
                }
            }
        }
    }

    // ---- text format

    @Test
    void writesTheDocumentedFormat() {
        String expected = """
                lab3-memento 1
                areaWidth 30.0
                areaHeight 30.0
                initialPopulation 60
                initialInfected 0
                entryRatePerSecond 1.7
                minSpeed 0.3
                immuneProbability 0.0
                symptomaticProbability 0.5
                velocityChangeProbability 0.02
                maxTurnAngle 0.7853981633974483
                maxSpeedDelta 0.5
                tick 250
                rngState -3523870230921562327
                nextPersonId 64
                persons 4
                person 0 10.5 20.25 -1.0 0.5 HEALTHY 0
                person 3 4.0 5.0 0.0 1.5 INFECTED_SYMPTOMATIC 612
                person 9 4.5 5.0 0.25 -0.75 IMMUNE 0
                person 12 1.0 1.0 1.0 1.0 INFECTED_ASYMPTOMATIC 501
                contacts 2
                contact 0 3 17 false
                contact 0 12 79 true
                end
                """;

        assertEquals(expected, SnapshotStore.toText(small()));
    }

    @Test
    void textRoundTripGivesAnEqualMemento() throws SnapshotFormatException {
        SimulationMemento original = small();

        assertEquals(original, SnapshotStore.fromText(SnapshotStore.toText(original)));
    }

    @Test
    void textRoundTripKeepsEveryBitOfEveryNumberOfARealRun() throws SnapshotFormatException {
        for (long seed : new long[] {1, 2, 3}) {
            for (int ticks : new int[] {0, 1, 137, 800, 2600}) {
                SimulationMemento original = busy(seed, ticks);

                assertEquals(original, SnapshotStore.fromText(SnapshotStore.toText(original)),
                        "seed " + seed + ", tick " + ticks);
            }
        }
    }

    @Test
    void mementoReadFromTextContinuesLikeTheOriginalSimulation() throws SnapshotFormatException {
        Simulation original = Simulation.withSeed(SimulationConfig.noImmunity(), 21);
        for (int i = 0; i < 900; i++) {
            original.step();
        }
        SimulationMemento read = SnapshotStore.fromText(SnapshotStore.toText(original.createMemento()));
        Simulation copy = Simulation.fromMemento(read);

        for (int i = 0; i < 700; i++) {
            original.step();
            copy.step();
        }

        assertEquals(original.view(), copy.view());
        assertEquals(original.createMemento(), copy.createMemento());
    }

    @Test
    void windowsLineEndingsAndTrailingBlankLinesAreAccepted() throws SnapshotFormatException {
        String text = SnapshotStore.toText(small()).replace("\n", "\r\n") + "\r\n\r\n";

        assertEquals(small(), SnapshotStore.fromText(text));
    }

    @Test
    void emptyMementoWithoutPeopleRoundTrips() throws SnapshotFormatException {
        SimulationMemento empty = new SimulationMemento(SimulationConfig.withImmunity(0.4), 0, 0, 0, List.of(), List.of());

        assertEquals(empty, SnapshotStore.fromText(SnapshotStore.toText(empty)));
    }

    // ---- files

    @Test
    void saveAndLoadGiveAnEqualMemento() throws IOException {
        inTempDirectory(directory -> {
            Path file = directory.resolve("state.txt");
            SimulationMemento original = busy(5, 1000);

            SnapshotStore.save(file, original);

            assertTrue(Files.exists(file));
            assertEquals(original, SnapshotStore.load(file));
        });
    }

    @Test
    void savingReplacesAnExistingFile() throws IOException {
        inTempDirectory(directory -> {
            Path file = directory.resolve("state.txt");
            SnapshotStore.save(file, busy(5, 1000));
            SimulationMemento newer = busy(5, 50);

            SnapshotStore.save(file, newer);

            assertEquals(newer, SnapshotStore.load(file));
        });
    }

    @Test
    void fileIsUtf8TextThatEndsWithEnd() throws IOException {
        inTempDirectory(directory -> {
            Path file = directory.resolve("state.txt");
            SnapshotStore.save(file, small());

            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);

            assertEquals("lab3-memento 1", lines.get(0));
            assertEquals("end", lines.get(lines.size() - 1));
        });
    }

    @Test
    void loadingAMissingFileIsAnIoError() throws IOException {
        inTempDirectory(directory -> assertThrows(NoSuchFileException.class,
                () -> SnapshotStore.load(directory.resolve("nothing.txt"))));
    }

    @Test
    void savingToAMissingFolderIsAnIoError() throws IOException {
        inTempDirectory(directory -> assertThrows(IOException.class,
                () -> SnapshotStore.save(directory.resolve("no-such-folder").resolve("state.txt"), small())));
    }

    @Test
    void emptyFileIsRejectedWithAFormatError() throws IOException {
        inTempDirectory(directory -> {
            Path file = directory.resolve("empty.txt");
            Files.writeString(file, "");

            assertThrows(SnapshotFormatException.class, () -> SnapshotStore.load(file));
        });
    }

    @Test
    void binaryGarbageIsRejectedWithAFormatError() throws IOException {
        inTempDirectory(directory -> {
            Path file = directory.resolve("garbage.bin");
            byte[] bytes = new byte[2000];
            new Random(1).nextBytes(bytes);
            Files.write(file, bytes);

            assertThrows(SnapshotFormatException.class, () -> SnapshotStore.load(file));
        });
    }

    @Test
    void tooLargeFileIsRejectedWithoutReadingIt() throws IOException {
        inTempDirectory(directory -> {
            Path file = directory.resolve("huge.txt");
            Files.write(file, new byte[(int) SnapshotStore.MAX_BYTES + 1]);

            SnapshotFormatException e = assertThrows(SnapshotFormatException.class, () -> SnapshotStore.load(file));
            assertTrue(e.getMessage().contains("too large"), e.getMessage());
        });
    }

    // ---- damaged content

    @Test
    void wrongHeaderIsRejected() {
        String text = SnapshotStore.toText(small());

        assertTrue(rejected(replaceLine(text, 1, "something-else 1")).getMessage().startsWith("line 1:"));
        assertTrue(rejected(replaceLine(text, 1, "lab3-memento 2")).getMessage().contains("version"));
        assertTrue(rejected(replaceLine(text, 1, "lab3-memento")).getMessage().startsWith("line 1:"));
        rejected("");
        rejected("\n\n");
    }

    @Test
    void errorMessageNamesTheWrongLine() {
        String text = SnapshotStore.toText(small());

        assertTrue(rejected(replaceLine(text, 2, "areaWidth abc")).getMessage().startsWith("line 2:"));
        assertTrue(rejected(replaceLine(text, 18, "person 3 x 5.0 0.0 1.5 HEALTHY 0")).getMessage().startsWith("line 18:"));
    }

    @Test
    void missingOrReorderedLinesAreRejected() {
        String text = SnapshotStore.toText(small());

        rejected(removeLine(text, 2));
        rejected(removeLine(text, 13));
        rejected(removeLine(text, 24));
        rejected(replaceLine(replaceLine(text, 2, "areaHeight 30.0"), 3, "areaWidth 30.0"));
        rejected(text.substring(0, text.length() / 2));
    }

    @Test
    void numbersThatAreNotNumbersAreRejected() {
        String text = SnapshotStore.toText(small());

        rejected(replaceLine(text, 2, "areaWidth thirty"));
        rejected(replaceLine(text, 2, "areaWidth NaN"));
        rejected(replaceLine(text, 2, "areaWidth Infinity"));
        rejected(replaceLine(text, 4, "initialPopulation 60.5"));
        rejected(replaceLine(text, 13, "tick 12x"));
        rejected(replaceLine(text, 14, "rngState 99999999999999999999999"));
        rejected(replaceLine(text, 18, "person 3 4.0 5.0 0.0 1.5 INFECTED_SYMPTOMATIC six"));
    }

    @Test
    void nonFiniteNumbersAreReportedOnTheirOwnLine() {
        String text = SnapshotStore.toText(small());

        SnapshotFormatException nan = rejected(replaceLine(text, 2, "areaWidth NaN"));
        assertTrue(nan.getMessage().startsWith("line 2:") && nan.getMessage().contains("not finite"), nan.getMessage());
        SnapshotFormatException infinite = rejected(replaceLine(text, 18, "person 3 Infinity 5.0 0.0 1.5 INFECTED_SYMPTOMATIC 612"));
        assertTrue(infinite.getMessage().startsWith("line 18:") && infinite.getMessage().contains("not finite"),
                infinite.getMessage());
    }

    @Test
    void wrongItemCountsOrEmptyItemsAreRejected() {
        String text = SnapshotStore.toText(small());

        rejected(replaceLine(text, 2, "areaWidth 30.0 31.0"));
        rejected(replaceLine(text, 2, "areaWidth"));
        rejected(replaceLine(text, 2, "areaWidth  30.0"));
        rejected(replaceLine(text, 2, "areaWidth 30.0 "));
        rejected(replaceLine(text, 18, "person 3 4.0 5.0 0.0 1.5 INFECTED_SYMPTOMATIC"));
        rejected(replaceLine(text, 18, "person 3 4.0 5.0 0.0 1.5 INFECTED_SYMPTOMATIC 612 7"));
        rejected(replaceLine(text, 22, "contact 0 3 17"));
    }

    @Test
    void unknownHealthKindAndBadBooleanAreRejected() {
        String text = SnapshotStore.toText(small());

        rejected(replaceLine(text, 18, "person 3 4.0 5.0 0.0 1.5 ZOMBIE 612"));
        rejected(replaceLine(text, 18, "person 3 4.0 5.0 0.0 1.5 infected_symptomatic 612"));
        rejected(replaceLine(text, 22, "contact 0 3 17 no"));
        rejected(replaceLine(text, 22, "contact 0 3 17 FALSE"));
    }

    @Test
    void declaredCountsThatDoNotMatchTheLinesAreRejected() {
        String text = SnapshotStore.toText(small());

        rejected(replaceLine(text, 16, "persons 5"));
        rejected(replaceLine(text, 16, "persons 3"));
        rejected(replaceLine(text, 16, "persons -1"));
        rejected(replaceLine(text, 16, "persons 2147483647"));
        rejected(replaceLine(text, 21, "contacts 3"));
        rejected(replaceLine(text, 21, "contacts 1"));
    }

    @Test
    void contentThatBreaksTheRulesOfTheMementoIsRejectedForTheRightReason() {
        String text = SnapshotStore.toText(small());

        // person lines no longer sorted by id
        String unsorted = replaceLine(replaceLine(text, 18, "person 12 4.0 5.0 0.0 1.5 HEALTHY 0"), 20, "person 3 1.0 1.0 1.0 1.0 HEALTHY 0");
        assertTrue(rejected(unsorted).getMessage().contains("ascending"), rejected(unsorted).getMessage());
        // infected person without remaining ticks, healthy person with remaining ticks
        assertTrue(rejected(replaceLine(text, 18, "person 3 4.0 5.0 0.0 1.5 INFECTED_SYMPTOMATIC 0"))
                .getMessage().startsWith("line 18:"));
        assertTrue(rejected(replaceLine(text, 18, "person 3 4.0 5.0 0.0 1.5 HEALTHY 5")).getMessage().startsWith("line 18:"));
        // contact with a person who does not exist
        assertTrue(rejected(replaceLine(text, 22, "contact 0 77 17 false")).getMessage().contains("does not exist"));
        // draw flag that does not match the count
        SnapshotFormatException drawn = rejected(replaceLine(text, 22, "contact 0 3 17 true"));
        assertTrue(drawn.getMessage().startsWith("line 22:") && drawn.getMessage().contains("drawn"), drawn.getMessage());
        // nextPersonId not above the largest id
        assertTrue(rejected(replaceLine(text, 15, "nextPersonId 12")).getMessage().contains("nextPersonId"));
        // negative tick
        assertTrue(rejected(replaceLine(text, 13, "tick -1")).getMessage().contains("tick"));
        // configuration the program would never accept
        for (String bad : new String[] {"areaWidth -5.0", "initialInfected 100", "immuneProbability 1.5"}) {
            int line = bad.startsWith("areaWidth") ? 2 : bad.startsWith("initialInfected") ? 5 : 8;
            SnapshotFormatException e = rejected(replaceLine(text, line, bad));
            assertTrue(e.getMessage().startsWith("lines 2-12: invalid configuration"), e.getMessage());
        }
    }

    @Test
    void textAfterEndIsRejectedButBlankLinesAreNot() throws SnapshotFormatException {
        String text = SnapshotStore.toText(small());

        rejected(text + "more\n");
        rejected(text + "\nend\n");
        assertEquals(small(), SnapshotStore.fromText(text + "\n   \n"));
    }

    @Test
    void anySingleCharacterDamageEndsInAFormatErrorOrAnEqualOrValidMemento() {
        String text = SnapshotStore.toText(busy(3, 600));
        Random random = new Random(7);
        char[] replacements = {'x', '9', '-', ' ', '.', 'e', '\n', '0', 'N'};

        for (int round = 0; round < 3000; round++) {
            StringBuilder damaged = new StringBuilder(text);
            int position = random.nextInt(damaged.length());
            switch (random.nextInt(3)) {
                case 0 -> damaged.setCharAt(position, replacements[random.nextInt(replacements.length)]);
                case 1 -> damaged.deleteCharAt(position);
                default -> damaged.insert(position, replacements[random.nextInt(replacements.length)]);
            }
            try {
                SnapshotStore.fromText(damaged.toString());
            } catch (SnapshotFormatException expected) {
                assertFalse(expected.getMessage() == null || expected.getMessage().isBlank());
            }
            // any other exception type would fail the test
        }
    }

    @Test
    void anyTruncationEndsInAFormatError() {
        String text = SnapshotStore.toText(small());

        for (int length = 0; length < text.length() - 1; length++) {
            String truncated = text.substring(0, length);
            assertThrows(SnapshotFormatException.class, () -> SnapshotStore.fromText(truncated), "length " + length);
        }
    }
}
