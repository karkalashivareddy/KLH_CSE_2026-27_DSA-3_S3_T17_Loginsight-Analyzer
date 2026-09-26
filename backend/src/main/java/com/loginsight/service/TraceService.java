package com.loginsight.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.loginsight.dsa.approximation.UndirectedGraph;
import com.loginsight.dsa.approximation.VertexCoverApproximation;
import com.loginsight.dsa.dp.editdistance.LevenshteinDistance;
import com.loginsight.dsa.dp.interval.MatrixChainMultiplication;
import com.loginsight.dsa.flow.Dinic;
import com.loginsight.dsa.flow.EdmondsKarp;
import com.loginsight.dsa.flow.FordFulkerson;
import com.loginsight.dsa.randomized.MillerRabin;
import com.loginsight.dsa.randomized.RandomSource;
import com.loginsight.dsa.randomized.RandomizedQuickSort;
import com.loginsight.dsa.randomized.ReservoirSampling;
import com.loginsight.dsa.string.KMPMatcher;
import com.loginsight.dsa.string.NaiveMatcher;
import com.loginsight.dsa.string.RabinKarpMatcher;
import com.loginsight.dsa.string.ZAlgorithm;
import com.loginsight.dto.request.DistanceRequest;
import com.loginsight.dto.request.FlowRequest;
import com.loginsight.dto.request.MatrixChainRequest;
import com.loginsight.dto.request.MillerRabinRequest;
import com.loginsight.dto.request.QuicksortRequest;
import com.loginsight.dto.request.ReservoirRequest;
import com.loginsight.dto.request.SearchRequest;
import com.loginsight.dto.request.VertexCoverRequest;
import com.loginsight.dto.response.TraceResponseDto;
import com.loginsight.exception.InvalidQueryException;
import com.loginsight.query.QueryValidator;
import com.loginsight.query.engine.approximation.VertexCoverGraphBuilder;
import com.loginsight.query.engine.flow.FlowGraphFactory;
import com.loginsight.trace.AlgorithmStep;
import com.loginsight.trace.TraceCatalog;
import com.loginsight.trace.TracedResult;

/**
 * Bridges the trace-capable DSA execution paths to the {@code /api/trace} REST surface.
 *
 * <p>Each method delegates to the real, trace-instrumented DSA algorithm (the same classes the
 * ordinary engines use) and maps the {@link TracedResult} to a {@link TraceResponseDto}, translating
 * integer vertex ids back to service names for flow/cover scenarios. Nothing here fabricates a step:
 * a step exists in the response only if the running algorithm recorded it.</p>
 */
@Service
public class TraceService {

    public List<LinkedHashMap<String, Object>> catalog() {
        return TraceCatalog.all();
    }

    public TraceResponseDto naive(SearchRequest request) {
        return toDto("naive", "Strings",
                new NaiveMatcher().matchTracked(explicitText(request), request.pattern()));
    }

    public TraceResponseDto kmp(SearchRequest request) {
        return toDto("kmp", "Strings",
                new KMPMatcher().matchTracked(explicitText(request), request.pattern()));
    }

    public TraceResponseDto z(SearchRequest request) {
        return toDto("z", "Strings",
                new ZAlgorithm().matchTracked(explicitText(request), request.pattern()));
    }

    public TraceResponseDto rabinKarp(SearchRequest request) {
        boolean doubleHash = Boolean.TRUE.equals(request.doubleHash());
        return toDto("rabinkarp", "Strings",
                new RabinKarpMatcher().matchTracked(explicitText(request), request.pattern(),
                        doubleHash));
    }

    public TraceResponseDto levenshtein(DistanceRequest request) {
        return toDto("levenshtein", "Dynamic Programming",
                new LevenshteinDistance().matchTracked(request.a(), request.b()));
    }

    public TraceResponseDto matrixChain(MatrixChainRequest request) {
        return toDto("matrixchain", "Dynamic Programming",
                new MatrixChainMultiplication().solveTracked(request.dims()));
    }

    public TraceResponseDto fordFulkerson(FlowRequest request) {
        FlowGraphFactory.NamedFlow named = FlowGraphFactory.build(request);
        return toDto("fordfulkerson", "Graph & Flow",
                new FordFulkerson().maxFlowTracked(named.graph(), named.source(), named.sink()),
                named.names());
    }

    public TraceResponseDto edmondsKarp(FlowRequest request) {
        FlowGraphFactory.NamedFlow named = FlowGraphFactory.build(request);
        return toDto("edmondskarp", "Graph & Flow",
                new EdmondsKarp().maxFlowTracked(named.graph(), named.source(), named.sink()),
                named.names());
    }

    public TraceResponseDto dinic(FlowRequest request) {
        FlowGraphFactory.NamedFlow named = FlowGraphFactory.build(request);
        return toDto("dinic", "Graph & Flow",
                new Dinic().maxFlowTracked(named.graph(), named.source(), named.sink()),
                named.names());
    }

    public TraceResponseDto vertexCover(VertexCoverRequest request) {
        VertexCoverGraphBuilder.Build build = VertexCoverGraphBuilder.build(request);
        return toDto("vertexcover", "Approximation",
                new VertexCoverApproximation().approximateTracked(build.graph()), build.names());
    }

    public TraceResponseDto quicksort(QuicksortRequest request) {
        long[] values = request.values() == null ? new long[0] : request.values();
        long seed = request.seed() == null ? 42L : request.seed();
        return toDto("quicksort", "Randomized",
                new RandomizedQuickSort().sortTracked(values, RandomSource.seeded(seed)));
    }

    public TraceResponseDto millerRabin(MillerRabinRequest request) {
        long n = request.n() == null ? 0 : request.n();
        int rounds = request.rounds() == null ? 20 : request.rounds();
        MillerRabin.Mode mode = useDeterministic(request) ? MillerRabin.Mode.DETERMINISTIC
                : MillerRabin.Mode.PROBABILISTIC;
        return toDto("millerrabin", "Randomized",
                new MillerRabin().testTracked(n, mode, RandomSource.seeded(42L), rounds));
    }

    public TraceResponseDto reservoir(ReservoirRequest request) {
        int k = request.k() == null ? 10 : request.k();
        if (k < 0) {
            throw new InvalidQueryException("k must be >= 0, got " + k);
        }
        k = Math.min(k, QueryValidator.MAX_RESERVOIR_K);
        long[] stream = streamOf(request);
        return toDto("reservoir", "Randomized",
                new ReservoirSampling(k, RandomSource.unseeded()).sampleTracked(stream));
    }

    /**
     * The traced stream: the explicit {@code values} array when present, otherwise the
     * {@code size}-token stream {@code [0, size)} the canonical endpoint samples from, so a run
     * without {@code values} still shows a real Algorithm R trace instead of an empty one.
     */
    private static long[] streamOf(ReservoirRequest request) {
        if (request.values() != null && request.values().length > 0) {
            if (request.values().length > QueryValidator.MAX_RESERVOIR_VALUES) {
                throw new InvalidQueryException("values must hold at most "
                        + QueryValidator.MAX_RESERVOIR_VALUES + " elements");
            }
            return request.values();
        }
        int size = request.size() == null ? 0 : request.size();
        int capped = Math.max(0, Math.min(size, QueryValidator.MAX_RESERVOIR_STREAM));
        long[] stream = new long[capped];
        for (int i = 0; i < capped; i++) {
            stream[i] = i;
        }
        return stream;
    }

    private static boolean useDeterministic(MillerRabinRequest request) {
        Integer rounds = request.rounds();
        return rounds == null || rounds <= 0;
    }

    private static String explicitText(SearchRequest request) {
        String text = request.text();
        if (text == null || text.isBlank()) {
            throw new InvalidQueryException("trace search endpoints require an explicit text haystack");
        }
        return text;
    }

    private static TraceResponseDto toDto(String key, String category, TracedResult traced) {
        return new TraceResponseDto(traced.algorithm(), category, traced.result(),
                traced.finalIntermediate(), stepsToWire(traced.steps()), truncatedOf(traced),
                traced.executionTimeNanos(), traced.timeComplexity(), traced.spaceComplexity());
    }

    private static TraceResponseDto toDto(String key, String category, TracedResult traced,
                                          String[] names) {
        return new TraceResponseDto(traced.algorithm(), category, remap(traced.result(), names),
                remap(traced.finalIntermediate(), names), stepsToWire(traced.steps(), names),
                truncatedOf(traced), traced.executionTimeNanos(), traced.timeComplexity(),
                traced.spaceComplexity());
    }

    private static boolean truncatedOf(TracedResult traced) {
        return traced.finalIntermediate() instanceof Map<?, ?> map
                && Boolean.TRUE.equals(map.get("truncated"));
    }

    private static List<Map<String, Object>> stepsToWire(List<AlgorithmStep> steps) {
        List<Map<String, Object>> out = new ArrayList<>(steps.size());
        for (AlgorithmStep step : steps) {
            out.add(stepToMap(step));
        }
        return out;
    }

    private static List<Map<String, Object>> stepsToWire(List<AlgorithmStep> steps, String[] names) {
        List<Map<String, Object>> out = new ArrayList<>(steps.size());
        for (AlgorithmStep step : steps) {
            Map<String, Object> map = stepToMap(step);
            Object vertices = step.state().get("pathVertices");
            if (vertices instanceof List<?> list) {
                @SuppressWarnings("unchecked")
                Map<String, Object> state = new LinkedHashMap<>(
                        (Map<String, Object>) map.get("state"));
                state.put("pathVertices", mapVertexIds(list, names));
                map.put("state", state);
            }
            out.add(map);
        }
        return out;
    }

    private static Map<String, Object> stepToMap(AlgorithmStep step) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("index", step.index());
        out.put("operation", step.operation());
        out.put("description", step.description());
        out.put("state", step.state());
        out.put("highlighted", step.highlighted());
        out.put("metrics", step.metrics());
        return out;
    }

    private static List<String> mapVertexIds(List<?> ids, String[] names) {
        List<String> out = new ArrayList<>(ids.size());
        for (Object id : ids) {
            int v = ((Number) id).intValue();
            out.add(v >= 0 && v < names.length ? names[v] : String.valueOf(v));
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static Object remap(Object payload, String[] names) {
        if (payload instanceof Map<?, ?> map) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if ("pathVertices".equals(e.getKey()) && e.getValue() instanceof List<?> list) {
                    out.put((String) e.getKey(), mapVertexIds(list, names));
                } else {
                    out.put(String.valueOf(e.getKey()), e.getValue());
                }
            }
            out.put("names", java.util.Arrays.asList(names));
            return out;
        }
        return payload;
    }
}