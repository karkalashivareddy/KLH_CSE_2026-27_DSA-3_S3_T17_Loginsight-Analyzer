package com.loginsight.catalog;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.loginsight.model.AlgorithmType;
import com.loginsight.model.QueryType;
import com.loginsight.query.QueryEngine;
import com.loginsight.query.engine.approximation.IncidentCoverEngine;
import com.loginsight.query.engine.approximation.SetCoverEngine;
import com.loginsight.query.engine.approximation.VertexCoverEngine;
import com.loginsight.query.engine.dp.BitmaskTspEngine;
import com.loginsight.query.engine.dp.DamerauEngine;
import com.loginsight.query.engine.dp.GlobalAlignmentEngine;
import com.loginsight.query.engine.dp.HamiltonianEngine;
import com.loginsight.query.engine.dp.LevenshteinEngine;
import com.loginsight.query.engine.dp.LocalAlignmentEngine;
import com.loginsight.query.engine.dp.MatrixChainEngine;
import com.loginsight.query.engine.dp.OptimalBstEngine;
import com.loginsight.query.engine.dp.RerootingEngine;
import com.loginsight.query.engine.dp.SosDpEngine;
import com.loginsight.query.engine.dp.TreeDpEngine;
import com.loginsight.query.engine.dp.WeightedEditEngine;
import com.loginsight.query.engine.flow.DinicEngine;
import com.loginsight.query.engine.flow.EdmondsKarpEngine;
import com.loginsight.query.engine.flow.FordFulkersonEngine;
import com.loginsight.query.engine.flow.MatchingEngine;
import com.loginsight.query.engine.flow.MinCostEngine;
import com.loginsight.query.engine.flow.MinCutEngine;
import com.loginsight.query.engine.parallel.ReduceEngine;
import com.loginsight.query.engine.parallel.ScanEngine;
import com.loginsight.query.engine.parallel.SortEngine;
import com.loginsight.query.engine.randomized.MillerRabinEngine;
import com.loginsight.query.engine.randomized.QuicksortEngine;
import com.loginsight.query.engine.randomized.ReservoirEngine;
import com.loginsight.query.engine.randomized.UniversalHashEngine;
import com.loginsight.query.engine.string.AhoCorasickEngine;
import com.loginsight.query.engine.string.FuzzySearchEngine;
import com.loginsight.query.engine.string.KmpSearchEngine;
import com.loginsight.query.engine.string.NaiveSearchEngine;
import com.loginsight.query.engine.string.RabinKarpSearchEngine;
import com.loginsight.query.engine.string.SuffixEngine;
import com.loginsight.query.engine.string.ZSearchEngine;

/**
 * Catalogue metadata is validated against the implementation, not against prose: every declared
 * {@code queryType} / {@code algorithmType} must be a real enum constant, and for every algorithm
 * that a {@link QueryEngine} actually serves it must be the constant that engine reports. The
 * complexity statements are pinned to the audited claims of the owning DSA class.
 */
class AlgorithmCatalogMetadataTest {

    private static final Map<String, QueryEngine> OWNING_ENGINES = owningEngines();

    @Test
    void everyDeclaredTypeIsARealEnumConstant() {
        for (AlgorithmInfo info : AlgorithmCatalog.algorithms()) {
            assertDoesNotThrow(() -> QueryType.valueOf(info.queryType()),
                    info.key() + " declares query type " + info.queryType()
                            + " which is not a QueryType constant");
            assertDoesNotThrow(() -> AlgorithmType.valueOf(info.algorithmType()),
                    info.key() + " declares algorithm type " + info.algorithmType()
                            + " which is not an AlgorithmType constant");
        }
    }

    @Test
    void declaredTypesMatchTheEngineThatServesTheAlgorithm() {
        for (Map.Entry<String, QueryEngine> entry : OWNING_ENGINES.entrySet()) {
            AlgorithmInfo info = AlgorithmCatalog.byKey(entry.getKey()).orElseThrow();
            QueryEngine engine = entry.getValue();
            assertEquals(engine.type().name(), info.queryType(),
                    info.key() + " query type contradicts " + engine.getClass().getSimpleName());
            assertEquals(engine.algorithm().name(), info.algorithmType(),
                    info.key() + " algorithm type contradicts " + engine.getClass().getSimpleName());
        }
    }

    @Test
    void parallelScanIsRoutedAsParallelScanNotPrefixScan() {
        assertEquals(QueryType.PARALLEL_SCAN, QueryType.valueOf(info("parallel_scan").queryType()));
        assertEquals(AlgorithmType.PARALLEL_PREFIX_SCAN,
                AlgorithmType.valueOf(info("parallel_scan").algorithmType()));
    }

    @Test
    void incidentCoverIsLabelledWithItsMaximalMatchingEngine() {
        assertEquals(QueryType.APPROXIMATE_COVER,
                QueryType.valueOf(info("incident_cover").queryType()));
        assertEquals(AlgorithmType.MAXIMAL_MATCHING,
                AlgorithmType.valueOf(info("incident_cover").algorithmType()));
    }

    @Test
    void hamiltonianIsNamedForTheImplementedPath() {
        AlgorithmInfo info = info("hamiltonian");
        assertTrue(info.name().contains("Hamiltonian Path"),
                "hamiltonian name must name the implemented path, was: " + info.name());
        assertTrue(info.problem().toLowerCase().contains("path"),
                "hamiltonian problem must name the implemented path, was: " + info.problem());
        assertFalse(info.name().toLowerCase().contains("cycle"),
                "hamiltonian must not claim a cycle the implementation never searches for");
        assertEquals(AlgorithmType.HAMILTONIAN_PATH, AlgorithmType.valueOf(info.algorithmType()));
        assertEquals("O(2^n·n²)", info.timeComplexity());
        assertEquals("O(2^n·n)", info.spaceComplexity());
    }

    @Test
    void kernelizationTimeMatchesTheOwningWorstCase() {
        assertEquals("O(V·E)", info("vertex_cover_kernelization").timeComplexity());
        assertEquals("O(V+E)", info("vertex_cover_kernelization").spaceComplexity());
    }

    @Test
    void fuzzySearchSpaceMatchesTheFullWagnerFischerTable() {
        assertEquals("O(lines · |q|)", info("fuzzy_search").timeComplexity());
        assertEquals("O(line · |q|) DP table", info("fuzzy_search").spaceComplexity());
    }

    @Test
    void everyTraceableAlgorithmCarriesAUsableDefaultInput() {
        List<AlgorithmInfo> tracked = AlgorithmCatalog.algorithms().stream()
                .filter(AlgorithmInfo::tracked)
                .toList();
        assertEquals(13, tracked.size(), "expected 13 trace-instrumented algorithms");
        for (AlgorithmInfo info : tracked) {
            assertNotNull(info.defaultInput(),
                    info.key() + " is traceable and must expose a runnable default input");
            assertTrue(info.defaultInput() instanceof Map,
                    info.key() + " default input must be a request-body object");
            assertFalse(((Map<?, ?>) info.defaultInput()).isEmpty(),
                    info.key() + " default input must not be empty");
        }
    }

    @Test
    void libraryOnlyEntriesHaveNoDefaultInputToRun() {
        for (String key : List.of("kasai_lcp", "perfect_hash")) {
            assertNull(info(key).defaultInput(), key + " is library-only and has no runnable input");
        }
    }

    private static AlgorithmInfo info(String key) {
        return AlgorithmCatalog.byKey(key).orElseThrow(
                () -> new IllegalStateException("missing catalogue entry " + key));
    }

    private static Map<String, QueryEngine> owningEngines() {
        Map<String, QueryEngine> engines = new LinkedHashMap<>();
        engines.put("naive", new NaiveSearchEngine());
        engines.put("kmp", new KmpSearchEngine());
        engines.put("z", new ZSearchEngine());
        engines.put("rabinkarp", new RabinKarpSearchEngine());
        engines.put("aho_corasick", new AhoCorasickEngine());
        engines.put("suffix_array", new SuffixEngine());
        engines.put("fuzzy_search", new FuzzySearchEngine());
        engines.put("levenshtein", new LevenshteinEngine());
        engines.put("damerau", new DamerauEngine());
        engines.put("weighted_edit", new WeightedEditEngine());
        engines.put("global_alignment", new GlobalAlignmentEngine());
        engines.put("local_alignment", new LocalAlignmentEngine());
        engines.put("matrixchain", new MatrixChainEngine());
        engines.put("optimal_bst", new OptimalBstEngine());
        engines.put("bitmask_tsp", new BitmaskTspEngine());
        engines.put("hamiltonian", new HamiltonianEngine());
        engines.put("tree_diameter", new TreeDpEngine());
        engines.put("rerooting", new RerootingEngine());
        engines.put("sos", new SosDpEngine());
        engines.put("fordfulkerson", new FordFulkersonEngine());
        engines.put("edmondskarp", new EdmondsKarpEngine());
        engines.put("dinic", new DinicEngine());
        engines.put("min_cut", new MinCutEngine());
        engines.put("bipartite_matching", new MatchingEngine());
        engines.put("min_cost_max_flow", new MinCostEngine());
        engines.put("vertexcover", new VertexCoverEngine());
        engines.put("incident_cover", new IncidentCoverEngine());
        engines.put("set_cover", new SetCoverEngine());
        engines.put("quicksort", new QuicksortEngine());
        engines.put("millerrabin", new MillerRabinEngine());
        engines.put("reservoir", new ReservoirEngine());
        engines.put("universal_hash", new UniversalHashEngine());
        engines.put("parallel_reduce", new ReduceEngine());
        engines.put("parallel_scan", new ScanEngine());
        engines.put("parallel_sort", new SortEngine());
        return Map.copyOf(engines);
    }
}
