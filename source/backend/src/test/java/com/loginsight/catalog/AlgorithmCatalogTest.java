package com.loginsight.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Catalogue integrity (docs/archive/REBUILD_BASELINE Phase-2): the metadata must mirror the real
 * implementation, so keys are unique, modules well-formed, and the tracked/exposed flags agree
 * with the endpoint columns.
 */
class AlgorithmCatalogTest {

    private static final Set<String> MODULE_IDS = Set.of("strings", "dp", "flow",
            "approximation", "randomized", "parallel");

    private static final Set<String> TRACEABLE_KEYS = Set.of("naive", "kmp", "z", "rabinkarp",
            "levenshtein", "matrixchain", "fordfulkerson", "edmondskarp", "dinic", "vertexcover",
            "quicksort", "millerrabin", "reservoir");

    @Test
    void keysAreUniqueAndComplete() {
        List<AlgorithmInfo> all = AlgorithmCatalog.algorithms();
        Set<String> keys = new HashSet<>();
        for (AlgorithmInfo info : all) {
            assertTrue(keys.add(info.key()), "duplicate key " + info.key());
        }
        assertTrue(all.size() >= 40, "expected at least 40 implemented algorithms");
        for (String key : TRACEABLE_KEYS) {
            assertTrue(keys.contains(key), "missing traceable algorithm entry " + key);
        }
    }

    @Test
    void modulesAreWellFormed() {
        long recognized = AlgorithmCatalog.algorithms().stream()
                .filter(info -> MODULE_IDS.contains(info.moduleId()))
                .count();
        assertEquals(AlgorithmCatalog.algorithms().size(), recognized,
                "every algorithm must belong to a known module");
    }

    @Test
    void trackedFlagAgreesWithTraceEndpoint() {
        for (AlgorithmInfo info : AlgorithmCatalog.algorithms()) {
            if (info.tracked()) {
                assertNotNull(info.traceEndpoint(),
                        info.key() + " is tracked but has no trace endpoint");
            }
            if (info.traceEndpoint() != null) {
                assertTrue(info.tracked(),
                        info.key() + " has a trace endpoint but is not tracked");
            }
            assertEquals(info.canonicalEndpoint() != null || info.traceEndpoint() != null,
                    info.exposed(), info.key() + " exposed flag inconsistent with endpoints");
        }
    }

    @Test
    void libraryOnlyEntriesAreNotExposed() {
        for (String key : List.of("kasai_lcp", "bounded_vertex_cover",
                "vertex_cover_kernelization", "knapsack_fptas", "vc_is_reduction",
                "perfect_hash")) {
            AlgorithmInfo info = AlgorithmCatalog.byKey(key).orElseThrow();
            assertFalse(info.exposed(), key + " must be marked exposed=false (library-only)");
            assertTrue(info.canonicalEndpoint() == null && info.traceEndpoint() == null,
                    key + " claims endpoints it does not have");
        }
    }

    @Test
    void complexitiesAreNeverBlank() {
        for (AlgorithmInfo info : AlgorithmCatalog.algorithms()) {
            assertNotNull(info.timeComplexity());
            assertNotNull(info.spaceComplexity());
            assertFalse(info.timeComplexity().isBlank());
            assertFalse(info.spaceComplexity().isBlank());
        }
    }

    @Test
    void moduleCountsCanBeDerived() {
        List<AlgorithmInfo> strings = AlgorithmCatalog.algorithms().stream()
                .filter(info -> info.moduleId().equals("strings"))
                .toList();
        assertEquals(9, strings.size(), "string module must hold 9 algorithms");
        assertTrue(strings.stream().anyMatch(AlgorithmInfo::tracked),
                "string module must expose trace-instrumented algorithms");
    }

    /**
     * Pins the four catalogue figures the README and docs publish: 42 entries, 36 reachable, 13
     * traceable, and 35 distinct dispatch keys.
     *
     * <p>These were previously only asserted as loose lower bounds, so the published numbers could
     * drift from the code without any test failing — which is exactly the kind of claim this project
     * should not make. They are pinned exactly here, and the two reconciling facts are asserted
     * directly rather than left as prose:</p>
     *
     * <ul>
     *   <li>42 − 36 is the six library-only entries, already pinned by
     *       {@link #libraryOnlyEntriesAreNotExposed()}, so reachable + library-only = total;</li>
     *   <li>36 − 35 is the one dispatch key shared by two catalogue entries, asserted below by
     *       naming the key, so a second collision cannot be absorbed silently.</li>
     * </ul>
     */
    @Test
    void publishedCatalogueFiguresMatchTheImplementation() {
        List<AlgorithmInfo> all = AlgorithmCatalog.algorithms();

        long exposed = all.stream().filter(AlgorithmInfo::exposed).count();
        long tracked = all.stream().filter(AlgorithmInfo::tracked).count();
        long libraryOnly = all.stream().filter(info -> !info.exposed()).count();
        long keys = all.stream()
                .filter(info -> info.canonicalEndpoint() != null || info.traceEndpoint() != null)
                .map(info -> info.queryType() + "|" + info.algorithmType())
                .distinct()
                .count();

        assertEquals(42, all.size(), "catalogue entry count changed; update README and docs");
        assertEquals(36, exposed, "reachable entry count changed; update README and docs");
        assertEquals(13, tracked, "traceable entry count changed; update README and docs");
        assertEquals(35, keys, "dispatch key count changed; update README and docs");

        assertEquals(all.size(), exposed + libraryOnly,
                "every entry is either reachable or library-only");

        // Exactly one key serves two entries: suffix build and suffix search are one algorithm.
        Map<String, List<String>> byKey = new LinkedHashMap<>();
        for (AlgorithmInfo info : all) {
            if (info.canonicalEndpoint() == null && info.traceEndpoint() == null) {
                continue;
            }
            byKey.computeIfAbsent(info.queryType() + "|" + info.algorithmType(),
                    k -> new ArrayList<>()).add(info.key());
        }
        List<String> shared = byKey.entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
        assertEquals(List.of("SUFFIX_ANALYSIS|SUFFIX_ARRAY"), shared,
                "the only shared dispatch key must stay suffix_array/suffix_search");
        assertEquals(List.of("suffix_array", "suffix_search"),
                byKey.get("SUFFIX_ANALYSIS|SUFFIX_ARRAY"),
                "suffix build and query are the two entries behind the shared key");
    }
}