package lab3.core;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the rule "logic is separated from UI": {@code core} and {@code vectors} must not touch
 * UI, files, system clock, threads or unseeded randomness. Sources are scanned as text, with
 * comments removed first.
 */
class ArchitectureTest {
    private static final Path SOURCES = Path.of("src", "main", "java");

    private static final List<Pattern> COMMON_RULES = List.of(
            Pattern.compile("\\bjavafx\\."),
            Pattern.compile("\\bjava\\.awt\\."),
            Pattern.compile("\\bjavax\\.swing\\."),
            Pattern.compile("\\bjava\\.io\\.File\\w*"),
            Pattern.compile("\\bjava\\.nio\\.file\\."),
            Pattern.compile("\\blab3\\.(ui|io|cli)\\b"),
            Pattern.compile("\\bjava\\.util\\.Random\\b"),
            Pattern.compile("\\bjava\\.util\\.concurrent\\."),
            Pattern.compile("\\bMath\\.random\\s*\\("),
            Pattern.compile("\\bSystem\\.(currentTimeMillis|nanoTime)\\s*\\("),
            Pattern.compile("\\bnew\\s+Thread\\b"));

    private static final List<Pattern> VECTORS_RULES = concat(COMMON_RULES, List.of(
            Pattern.compile("\\blab3\\.core\\b")));

    private static <T> List<T> concat(List<T> a, List<T> b) {
        List<T> result = new ArrayList<>(a);
        result.addAll(b);
        return result;
    }

    static String stripComments(String source) {
        return source
                .replaceAll("(?s)/\\*.*?\\*/", " ")
                .replaceAll("//[^\\n]*", " ");
    }

    static List<String> violations(String source, List<Pattern> rules) {
        String code = stripComments(source);
        List<String> found = new ArrayList<>();
        for (Pattern rule : rules) {
            if (rule.matcher(code).find()) {
                found.add(rule.pattern());
            }
        }
        return found;
    }

    private static List<String> scan(String packageDir, List<Pattern> rules) {
        Path dir = SOURCES.resolve(packageDir);
        assertTrue(Files.isDirectory(dir),
                "Cannot find " + dir.toAbsolutePath() + " - run tests with the project root as working directory");
        List<String> problems = new ArrayList<>();
        try (Stream<Path> files = Files.walk(dir)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file, StandardCharsets.UTF_8);
                for (String rule : violations(source, rules)) {
                    problems.add(file + " violates " + rule);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return problems;
    }

    @Test
    void coreHasNoUiIoClockThreadOrUnseededRandomness() {
        assertEquals(List.of(), scan("lab3/core", COMMON_RULES));
    }

    @Test
    void vectorsAreIndependentOfCoreAndUi() {
        assertEquals(List.of(), scan("lab3/vectors", VECTORS_RULES));
    }

    @Test
    void scannerDetectsForbiddenImports() {
        assertEquals(1, violations("import javafx.scene.Node;", COMMON_RULES).size());
        assertEquals(1, violations("import javax.swing.JFrame;", COMMON_RULES).size());
        assertEquals(1, violations("import java.nio.file.Path;", COMMON_RULES).size());
        assertEquals(1, violations("import lab3.ui.MainView;", COMMON_RULES).size());
        assertEquals(1, violations("import java.util.Random;", COMMON_RULES).size());
    }

    @Test
    void scannerDetectsForbiddenCalls() {
        assertEquals(1, violations("double d = Math.random();", COMMON_RULES).size());
        assertEquals(1, violations("long t = System.currentTimeMillis();", COMMON_RULES).size());
        assertEquals(1, violations("new Thread(r).start();", COMMON_RULES).size());
    }

    @Test
    void scannerIgnoresComments() {
        String source = """
                /** Unlike {@link java.util.Random}, the state is a single long. */
                // import javafx.scene.Node;
                class A { /* Math.random() */ }
                """;

        assertEquals(List.of(), violations(source, COMMON_RULES));
    }

    @Test
    void scannerAllowsSerializableAndPlainCode() {
        String source = "import java.io.Serializable;\nclass A implements Serializable { double x = 1.0; }";

        assertEquals(List.of(), violations(source, COMMON_RULES));
    }

    @Test
    void vectorsRuleDetectsCoreImport() {
        assertEquals(1, violations("import lab3.core.Ticks;", VECTORS_RULES).size());
        assertEquals(List.of(), violations("import lab3.core.Ticks;", COMMON_RULES));
    }
}
