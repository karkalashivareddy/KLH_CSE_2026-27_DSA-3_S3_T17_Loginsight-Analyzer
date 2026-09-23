package com.loginsight.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Catalogue integrity (docs/REBUILD_BASELINE Phase-2): the metadata must mirror the real
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
}