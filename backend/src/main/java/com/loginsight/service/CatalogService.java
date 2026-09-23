package com.loginsight.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.loginsight.catalog.AlgorithmCatalog;
import com.loginsight.catalog.AlgorithmInfo;
import com.loginsight.catalog.ModuleInfo;

/**
 * Read-only service over the algorithm catalogue (docs/REBUILD_BASELINE Phase-2). Computed module
 * counts (implemented / exposed / traceable) are derived from {@link AlgorithmCatalog} so they
 * cannot drift from the data.
 */
@Service
public class CatalogService {

    /** Ordering of the laboratory sidebar. */
    private static final List<String> MODULE_ORDER = List.of("strings", "dp", "flow",
            "approximation", "randomized", "parallel");

    private static final List<ModuleInfo> MODULES = buildModules();

    public List<ModuleInfo> modules() {
        return MODULES;
    }

    public Optional<ModuleInfo> module(String id) {
        for (ModuleInfo module : MODULES) {
            if (module.id().equals(id)) {
                return Optional.of(module);
            }
        }
        return Optional.empty();
    }

    public Optional<AlgorithmInfo> algorithm(String key) {
        return AlgorithmCatalog.byKey(key);
    }

    public List<AlgorithmInfo> algorithms() {
        return AlgorithmCatalog.algorithms();
    }

    private static List<ModuleInfo> buildModules() {
        List<ModuleInfo> out = new ArrayList<>();
        for (String id : MODULE_ORDER) {
            out.add(moduleFor(id));
        }
        return List.copyOf(out);
    }

    private static ModuleInfo moduleFor(String id) {
        List<AlgorithmInfo> inModule = AlgorithmCatalog.algorithms().stream()
                .filter(a -> a.moduleId().equals(id))
                .toList();
        long exposed = inModule.stream().filter(AlgorithmInfo::exposed).count();
        long trackable = inModule.stream().filter(AlgorithmInfo::tracked).count();
        return switch (id) {
            case "strings" -> new ModuleInfo(id, "String Algorithms",
                    "M2 · String Algorithms",
                    "Linear-time pattern search (KMP, Z, Rabin-Karp, Aho-Corasick), suffix-array "
                            + "phrase search and bounded fuzzy matching over the log corpus.",
                    "#22d3ee", inModule.size(), (int) exposed, (int) trackable, inModule);
            case "dp" -> new ModuleInfo(id, "Advanced Dynamic Programming",
                    "M3 · Advanced Dynamic Programming",
                    "Edit distances with traceback, global/local alignment, interval DP "
                            + "(matrix-chain, OBST), bitmask DP (TSP, Hamiltonian) and tree/SOS DP.",
                    "#a78bfa", inModule.size(), (int) exposed, (int) trackable, inModule);
            case "flow" -> new ModuleInfo(id, "Network Flow", "M4 · Network Flow",
                    "Max-flow hierarchy (Ford-Fulkerson, Edmonds-Karp, Dinic), min-cut, bipartite "
                            + "matching and min-cost max-flow on service-topology graphs.",
                    "#fbbf24", inModule.size(), (int) exposed, (int) trackable, inModule);
            case "approximation" -> new ModuleInfo(id, "Approximation & NP-Completeness",
                    "M5 · Approximation & NP-Completeness",
                    "Vertex cover 2-approximation, greedy set cover, FPT branching, kernelization "
                            + "and an FPTAS knapsack - each labelled exact vs approximate.",
                    "#34d399", inModule.size(), (int) exposed, (int) trackable, inModule);
            case "randomized" -> new ModuleInfo(id, "Randomized Algorithms",
                    "M6 · Randomized Algorithms",
                    "Miller-Rabin primality, reservoir sampling, universal and perfect hashing, and "
                            + "randomized quicksort - with seeded reproducibility.",
                    "#f472b6", inModule.size(), (int) exposed, (int) trackable, inModule);
            case "parallel" -> new ModuleInfo(id, "Parallel Algorithms", "M6 · Parallel Algorithms",
                    "Work-stealing reduce / prefix scan / sample sort with speedup, parallelism and "
                            + "work-span analysis; benchmark harness at /api/benchmark/run.",
                    "#60a5fa", inModule.size(), (int) exposed, (int) trackable, inModule);
            default -> throw new IllegalStateException("unknown module " + id);
        };
    }
}