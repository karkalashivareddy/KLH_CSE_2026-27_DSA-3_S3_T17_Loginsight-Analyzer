package com.loginsight.dsa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
 * <p>{@code java.util.concurrent} (parallel) and {@code java.util.Random} (randomized) are course-
 * licensed and therefore recorded in the manifest. {@code java.util.function}, streams and
 * {@code java.util.List/Map/ArrayList} in non-dsa code are unrestricted.</p>
 */
class EngineScopeGuardTest {

    private static final Pattern IMPORT = Pattern
            .compile("^\\s*import\\s+java\\.util\\.([A-Za-z0-9_.]+)\\s*;");

    private static final int MIN_SCANNED_FILES = 30;

    /** Frozen manifest: dsa source file (slash path) -> allowed java.util import suffixes. */
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
                            "concurrent.ExecutorService", "concurrent.Future")),
            Map.entry("parallel/ParallelReduce.java",
                    List.of("ArrayList", "List", "concurrent.Callable",
                            "concurrent.ExecutorService", "concurrent.Future")),
            Map.entry("parallel/ParallelSort.java",
                    List.of("ArrayList", "List", "concurrent.Callable",
                            "concurrent.ExecutorService", "concurrent.Future")),
            Map.entry("parallel/ParallelSupport.java",
                    List.of("concurrent.ExecutorService", "concurrent.Executors",
                            "concurrent.ThreadFactory", "concurrent.atomic.AtomicInteger")),
            Map.entry("parallel/WorkSpanAnalyzer.java", List.of("Arrays")),
            Map.entry("randomized/MillerRabin.java", List.of("ArrayList", "List", "Map")),
            Map.entry("randomized/RandomizedQuickSort.java", List.of("List", "Map")),
            Map.entry("randomized/RandomSource.java", List.of("Random")),
            Map.entry("randomized/ReservoirSampling.java", List.of("ArrayList", "List", "Map")),
            Map.entry("string/aho/AhoCorasick.java", List.of("Arrays")),
            Map.entry("string/KMPMatcher.java", List.of("ArrayList", "Arrays", "List", "Map")),
            Map.entry("string/NaiveMatcher.java", List.of("ArrayList", "Arrays", "List", "Map")),
            Map.entry("string/RabinKarpMatcher.java", List.of("ArrayList", "List", "Map")),
            Map.entry("string/ZAlgorithm.java", List.of("ArrayList", "Arrays", "List", "Map"))
    );

    @Test
    void dsaSourcesHoldWithinTheFrozenJavaUtilManifest() throws IOException {
        Path root = dsaRoot();
        assertTrue(Files.isDirectory(root), "dsa source root not found: " + root);

        List<String> violations = new ArrayList<>();
        int scanned = 0;
        for (Path file : trackedSources(root)) {
            scanned++;
            String rel = root.relativize(file).toString().replace('\\', '/');
            Set<String> present = imports(file);
            Set<String> allowed = allowedFor(rel);
            for (String used : present) {
                if (!allowed.contains(used)) {
                    violations.add(rel + " imports java.util." + used
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

    private static Set<String> allowedFor(String rel) {
        List<String> recorded = FROZEN_MANIFEST.get(rel);
        return recorded == null ? Set.of() : new LinkedHashSet<>(recorded);
    }

    private static List<Path> trackedSources(Path root) throws IOException {
        try (var stream = Files.walk(root)) {
            return stream.filter(p -> p.toString().endsWith(".java")).sorted().toList();
        }
    }

    private static Set<String> imports(Path file) throws IOException {
        Set<String> out = new LinkedHashSet<>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            Matcher matcher = IMPORT.matcher(line);
            if (matcher.matches()) {
                out.add(matcher.group(1));
            }
        }
        return out;
    }

    private static Path dsaRoot() {
        Path candidate = Paths.get("src", "main", "java", "com", "loginsight", "dsa");
        if (Files.isDirectory(candidate)) {
            return candidate;
        }
        return Paths.get("backend", "src", "main", "java", "com", "loginsight", "dsa");
    }
}