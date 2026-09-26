package com.loginsight.dsa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * Course scope guard for the {@code dsa} package (docs/REBUILD_BASELINE Phase-5).
 *
 * <p>DSA-3 forbids {@code java.util} collections inside the hand-built algorithm engines. The
 * pre-rebuild codebase already imports a bounded set of {@code java.util} types (verified during the
 * Phase-1 forensic audit), so a full refactor was riskier than the honest contract below:</p>
 *
 * <ul>
 *   <li>the frozen manifest records today's exact {@code java.util} usage per source file;</li>
 *   <li>the build fails on any <em>new</em> {@code java.util} import in {@code dsa/**} (collections
 *       leaking back in), while permitting the recorded set;</li>
 *   <li>removing existing usage is always allowed, so the ledger can only shrink.</li>
 * </ul>
 *
 * <p>The guard reads source text rather than the compiled class file, so every syntactic route into
 * {@code java.util} has to be recognised. Four are detected:</p>
 *
 * <ol>
 *   <li>a named import — {@code import java.util.Arrays;}, recorded as {@code Arrays};</li>
 *   <li>a fully-qualified reference in code — {@code java.util.concurrent.ExecutionException}, also
 *       recorded as {@code concurrent.ExecutionException} so it composes with the named form;</li>
 *   <li>a static import — {@code import static java.util.Arrays.asList;}, always rejected;</li>
 *   <li>a wildcard import — {@code import java.util.concurrent.*;}, always rejected.</li>
 * </ol>
 *
 * <p>Static and wildcard imports carry no manifest escape on purpose: both pull in a whole namespace
 * rather than the one named type the manifest can honestly record, so they can never be pinned to a
 * single approved usage. Comments, javadoc and string/char literals (including text blocks) are
 * masked before the code scan, so prose such as "no {@code java.util} collection is used" is not
 * mistaken for usage.</p>
 *
 * <p>{@code java.util.concurrent} (parallel) and {@code java.util.Random} (randomized) are course-
 * licensed and therefore recorded in the manifest. {@code java.util.function}, streams and
 * {@code java.util.List/Map/ArrayList} in non-dsa code are unrestricted.</p>
 */
class EngineScopeGuardTest {

    private static final Pattern IMPORT = Pattern.compile("^\\s*import\\s+(static\\s+)?java\\.util\\.([^;]+);");

    private static final Pattern IMPORT_STATEMENT = Pattern.compile("^\\s*import\\s");

    private static final Pattern QUALIFIED_REFERENCE = Pattern.compile(
            "(?<![A-Za-z0-9_$.])java\\s*\\.\\s*util\\s*\\.\\s*"
                    + "([A-Za-z0-9_$]+(?:\\s*\\.\\s*[A-Za-z0-9_$]+)*)");

    private static final int MIN_SCANNED_FILES = 30;

    /** Frozen manifest: dsa source file (slash path) -> allowed java.util type suffixes. */
    private static final Map<String, List<String>> FROZEN_MANIFEST = Map.ofEntries(
            Map.entry("approximation/VertexCoverApproximation.java",
                    List.of("ArrayList", "List", "Map")),
            Map.entry("common/CustomQueue.java", List.of("NoSuchElementException")),
            Map.entry("common/CustomStack.java", List.of("NoSuchElementException")),
            Map.entry("dp/editdistance/LevenshteinDistance.java",
                    List.of("ArrayList", "Arrays", "List", "Map")),
            Map.entry("dp/interval/MatrixChainMultiplication.java",
                    List.of("ArrayList", "List", "Map")),
            Map.entry("flow/Dinic.java", List.of("ArrayList", "List", "Map")),
            Map.entry("flow/EdmondsKarp.java", List.of("ArrayList", "List", "Map")),
            Map.entry("flow/FordFulkerson.java", List.of("ArrayList", "List", "Map")),
            Map.entry("parallel/ParallelBenchmark.java",
                    List.of("ArrayList", "List", "Random")),
            Map.entry("parallel/ParallelPrefixScan.java",
                    List.of("ArrayList", "List", "concurrent.Callable",
                            "concurrent.ExecutionException", "concurrent.ExecutorService",
                            "concurrent.Future")),
            Map.entry("parallel/ParallelReduce.java",
                    List.of("ArrayList", "List", "concurrent.Callable",
                            "concurrent.ExecutionException", "concurrent.ExecutorService",
                            "concurrent.Future")),
            Map.entry("parallel/ParallelSort.java",
                    List.of("ArrayList", "List", "concurrent.Callable",
                            "concurrent.ExecutionException", "concurrent.ExecutorService",
                            "concurrent.Future")),
            Map.entry("parallel/ParallelSupport.java",
                    List.of("concurrent.ExecutorService", "concurrent.Executors",
                            "concurrent.ThreadFactory", "concurrent.atomic.AtomicInteger")),
            Map.entry("parallel/WorkSpanAnalyzer.java", List.of("Arrays")),
            Map.entry("randomized/MillerRabin.java", List.of("ArrayList", "List", "Map")),
            Map.entry("randomized/RandomizedQuickSort.java",
                    List.of("ArrayList", "List", "Map")),
            Map.entry("randomized/RandomSource.java", List.of("Random")),
            Map.entry("randomized/ReservoirSampling.java", List.of("ArrayList", "List", "Map")),
            Map.entry("string/StringSearchResult.java", List.of("Arrays")),
            Map.entry("string/aho/AhoCorasick.java", List.of("Arrays")),
            Map.entry("string/KMPMatcher.java", List.of("ArrayList", "Arrays", "List", "Map")),
            Map.entry("string/NaiveMatcher.java", List.of("ArrayList", "Arrays", "List", "Map")),
            Map.entry("string/RabinKarpMatcher.java", List.of("ArrayList", "List", "Map")),
            Map.entry("string/ZAlgorithm.java", List.of("ArrayList", "Arrays", "List", "Map"))
    );

    /**
     * java.util usage of one source file: {@code pinnable} are the type suffixes the manifest can
     * approve, {@code rejected} are unpinnable forms (static / wildcard imports) that are always a
     * violation.
     */
    private record Usages(Set<String> pinnable, List<String> rejected) {
    }

    @Test
    void dsaSourcesHoldWithinTheFrozenJavaUtilManifest() throws IOException {
        Path root = dsaRoot();
        assertTrue(Files.isDirectory(root), "dsa source root not found: " + root);

        List<String> violations = new ArrayList<>();
        int scanned = 0;
        for (Path file : trackedSources(root)) {
            scanned++;
            String rel = root.relativize(file).toString().replace('\\', '/');
            Usages usages = usages(Files.readString(file, StandardCharsets.UTF_8));
            Set<String> allowed = allowedFor(rel);
            for (String rejected : usages.rejected()) {
                violations.add(rel + " " + rejected + ". Wildcard and static java.util imports pull "
                        + "in a whole namespace and are never approved; import the single named "
                        + "type instead, or use a hand-built structure.");
            }
            for (String used : usages.pinnable()) {
                if (!allowed.contains(used)) {
                    violations.add(rel + " uses java.util." + used
                            + " which is not in the frozen manifest. If this is an intentional "
                            + "course decision, update EngineScopeGuardTest.FROZEN_MANIFEST; "
                            + "otherwise replace the stdlib type with a hand-built one.");
                }
            }
        }

        assertTrue(scanned >= MIN_SCANNED_FILES,
                "scope guard scanned " + scanned + " files; expected at least "
                        + MIN_SCANNED_FILES + " (is the source root wrong?)");
        assertEquals(List.of(), violations.stream().sorted().toList(),
                "dsa/** acquired new java.util usage beyond the frozen manifest");
    }

    @Test
    void namedImportsAndFullyQualifiedReferencesAreBothRecorded() {
        Usages usages = usages("""
                package com.loginsight.dsa;

                import java.util.ArrayList;
                import java.util.concurrent.ExecutionException;

                final class Sample {

                    private final List<String> out = new java.util.ArrayList<>();
                    private final java.util.concurrent.atomic.AtomicInteger counter =
                            new java.util.concurrent.atomic.AtomicInteger();

                    void run() throws ExecutionException {
                        out.add(String.valueOf(counter.incrementAndGet()));
                    }
                }
                """);

        assertEquals(List.of(), usages.rejected());
        assertEquals(Set.of("ArrayList", "concurrent.ExecutionException",
                "concurrent.atomic.AtomicInteger"), usages.pinnable());
    }

    @Test
    void staticAndWildcardImportsAreAlwaysRejected() {
        Usages staticImport = usages("""
                package com.loginsight.dsa;

                import static java.util.Arrays.asList;

                final class Sample {

                    List<String> run() {
                        return asList("a");
                    }
                }
                """);
        assertEquals(List.of("imports java.util statically (import static java.util.Arrays.asList)"),
                staticImport.rejected());

        Usages wildcardPackage = usages("""
                package com.loginsight.dsa;

                import java.util.concurrent.*;

                final class Sample {

                    void run(ExecutorService pool) {
                        pool.shutdown();
                    }
                }
                """);
        assertEquals(List.of("imports java.util.* (import java.util.concurrent.*)"),
                wildcardPackage.rejected());

        Usages wildcardRoot = usages("""
                package com.loginsight.dsa;

                import java.util.*;

                final class Sample {

                    void run(List<String> items) {
                        items.clear();
                    }
                }
                """);
        assertEquals(List.of("imports java.util.* (import java.util.*)"),
                wildcardRoot.rejected());
    }

    @Test
    void commentsJavadocAndLiteralsAreNotMistakenForUsage() {
        Usages usages = usages("""
                package com.loginsight.dsa;

                import java.util.List;

                /**
                 * No {@code java.util} collection is used here, and neither is
                 * {@link java.util.Map}; a {@code java.util.ArrayDeque} would hide the queue.
                 */
                // java.util.HashSet is mentioned only in this line comment.
                final class Sample {

                    private static final String NOTE = "call java.util.Collections.sort first";
                    private static final char DOT = '.';
                    private static final String BLOCK = "{\\\"java.util.TreeMap\\\": 1}";

                    private final List<String> out = new List<>();

                    String run() {
                        return NOTE + DOT + BLOCK + out;
                    }
                }
                """);

        assertEquals(List.of(), usages.rejected());
        assertEquals(Set.of("List"), usages.pinnable());
    }

    @Test
    void multiLineCommentsAndEscapedQuotesCannotHideUsage() {
        Usages usages = usages("""
                package com.loginsight.dsa;

                /*
                 * A block comment mentioning java.util.Arrays must stay invisible, even when it
                 * contains an unbalanced quote ' and a double quote ".
                 */
                final class Sample {

                    private static final String QUOTE = "escaped \\" and java.util.Arrays";

                    Object run() {
                        return QUOTE.isEmpty() ? new java.util.ArrayList<>() : null;
                    }
                }
                """);

        assertEquals(List.of(), usages.rejected());
        assertEquals(Set.of("ArrayList"), usages.pinnable());
    }

    @Test
    void nonJavaUtilPackagesAreOutOfScope() {
        Usages usages = usages("""
                package com.loginsight.dsa;

                import java.io.IOException;
                import java.nio.file.Path;

                final class Sample {

                    private final java.nio.file.Paths paths = null;
                    private final java.lang.String text = "";

                    void run() throws IOException {
                        paths.toString();
                        text.length();
                    }
                }
                """);

        assertEquals(List.of(), usages.rejected());
        assertEquals(Set.of(), usages.pinnable());
    }

    // ------------------------------------------------------------------ internals

    private static Set<String> allowedFor(String rel) {
        List<String> recorded = FROZEN_MANIFEST.get(rel);
        return recorded == null ? Set.of() : new LinkedHashSet<>(recorded);
    }

    private static List<Path> trackedSources(Path root) throws IOException {
        try (var stream = Files.walk(root)) {
            return stream.filter(p -> p.toString().endsWith(".java")).sorted().toList();
        }
    }

    private static Usages usages(String source) {
        Set<String> pinnable = new LinkedHashSet<>();
        List<String> rejected = new ArrayList<>();
        String masked = maskNonCode(source);
        for (String line : masked.split("\n", -1)) {
            Matcher matcher = IMPORT.matcher(line);
            if (!matcher.matches()) {
                continue;
            }
            boolean staticImport = matcher.group(1) != null;
            String reference = matcher.group(2).trim();
            if (reference.equals("*") || reference.endsWith(".*")) {
                rejected.add("imports java.util.* (import java.util." + reference + ")");
            } else if (staticImport) {
                rejected.add("imports java.util statically (import static java.util."
                        + reference + ")");
            } else {
                pinnable.add(typeSuffix(reference));
            }
        }
        Matcher matcher = QUALIFIED_REFERENCE.matcher(blankImportStatements(masked));
        while (matcher.find()) {
            pinnable.add(typeSuffix(matcher.group(1).replaceAll("\\s+", "")));
        }
        return new Usages(pinnable, rejected);
    }

    /**
     * Reduces the java.util type suffix a reference points at: lowercase segments are package
     * segments, the first capitalised segment is the type. {@code concurrent.atomic.AtomicInteger}
     * therefore stays whole while {@code Locale.ROOT} narrows to {@code Locale}.
     */
    private static String typeSuffix(String reference) {
        StringBuilder out = new StringBuilder();
        for (String segment : reference.split("\\.")) {
            if (out.length() > 0) {
                out.append('.');
            }
            out.append(segment);
            if (!segment.isEmpty() && Character.isUpperCase(segment.charAt(0))) {
                break;
            }
        }
        return out.toString();
    }

    /** Blanks comments, javadoc and string / char / text-block literals, keeping line structure. */
    private static String maskNonCode(String source) {
        StringBuilder out = new StringBuilder(source.length());
        int i = 0;
        int n = source.length();
        while (i < n) {
            char c = source.charAt(i);
            char next = i + 1 < n ? source.charAt(i + 1) : '\0';
            if (c == '/' && next == '/') {
                while (i < n && source.charAt(i) != '\n') {
                    out.append(' ');
                    i++;
                }
            } else if (c == '/' && next == '*') {
                out.append("  ");
                i += 2;
                while (i < n && !(source.charAt(i) == '*' && i + 1 < n
                        && source.charAt(i + 1) == '/')) {
                    out.append(source.charAt(i) == '\n' ? '\n' : ' ');
                    i++;
                }
                if (i < n) {
                    out.append("  ");
                    i += 2;
                }
            } else if (c == '"' && next == '"' && i + 2 < n && source.charAt(i + 2) == '"') {
                out.append("   ");
                i += 3;
                while (i < n) {
                    if (source.charAt(i) == '\\' && i + 1 < n) {
                        out.append("  ");
                        i += 2;
                        continue;
                    }
                    if (source.charAt(i) == '"' && i + 2 < n && source.charAt(i + 1) == '"'
                            && source.charAt(i + 2) == '"') {
                        out.append("   ");
                        i += 3;
                        break;
                    }
                    out.append(source.charAt(i) == '\n' ? '\n' : ' ');
                    i++;
                }
            } else if (c == '"' || c == '\'') {
                out.append(' ');
                i++;
                while (i < n && source.charAt(i) != c) {
                    if (source.charAt(i) == '\\' && i + 1 < n) {
                        out.append("  ");
                        i += 2;
                        continue;
                    }
                    if (source.charAt(i) == '\n') {
                        break;
                    }
                    out.append(' ');
                    i++;
                }
                if (i < n && source.charAt(i) == c) {
                    out.append(' ');
                    i++;
                }
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    /**
     * Import statements are already accounted for by the named-import branch; blanking them stops
     * the fully-qualified scan from reporting the same type twice.
     */
    private static String blankImportStatements(String masked) {
        StringBuilder out = new StringBuilder(masked.length());
        for (String line : masked.split("\n", -1)) {
            out.append(IMPORT_STATEMENT.matcher(line).find() ? " ".repeat(line.length()) : line);
            out.append('\n');
        }
        return out.toString();
    }

    private static Path dsaRoot() {
        Path candidate = Paths.get("src", "main", "java", "com", "loginsight", "dsa");
        if (Files.isDirectory(candidate)) {
            return candidate;
        }
        return Paths.get("backend", "src", "main", "java", "com", "loginsight", "dsa");
    }
}
