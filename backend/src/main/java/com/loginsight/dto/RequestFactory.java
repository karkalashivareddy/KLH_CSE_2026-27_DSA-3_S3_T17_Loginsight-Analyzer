package com.loginsight.dto;

import java.util.List;
import java.util.Map;

import com.loginsight.dto.request.AlignmentRequest;
import com.loginsight.dto.request.DistanceRequest;
import com.loginsight.dto.request.FlowRequest;
import com.loginsight.dto.request.FuzzyRequest;
import com.loginsight.dto.request.GraphEdge;
import com.loginsight.dto.request.MatrixChainRequest;
import com.loginsight.dto.request.MillerRabinRequest;
import com.loginsight.dto.request.QuicksortRequest;
import com.loginsight.dto.request.ReservoirRequest;
import com.loginsight.dto.request.Scope;
import com.loginsight.dto.request.SearchRequest;
import com.loginsight.dto.request.VertexCoverRequest;
import com.loginsight.exception.InvalidQueryException;

/**
 * Converts the free-form {@code Map} input of TextHack queries and run requests into the same typed
 * request records the canonical REST endpoints accept, so both surfaces share one validation path
 * (docs/REBUILD_BASELINE Phase-2/3). Guards keep every converted input within the laboratory
 * bounds: {@code maxElements} entries in arrays and {@code maxTextLength} chars in haystacks.
 */
public final class RequestFactory {

    public static final int MAX_ARRAY_ELEMENTS = 4096;
    public static final int MAX_TEXT_LENGTH = 1_000_000;
    /** Interval-DP (matrix chain) is O(n^3); 40 dimension entries keep one run comfortably bounded. */
    public static final int MAX_INTERVAL_DIMS = 40;

    private RequestFactory() {
    }

    public static SearchRequest search(Map<String, Object> input) {
        String pattern = str(input, "pattern");
        if (pattern == null || pattern.isBlank()) {
            throw new InvalidQueryException("pattern is required");
        }
        String text = boundedText(input, "text");
        Scope scope = scope(input);
        Boolean doubleHash = bool(input, "doubleHash");
        return new SearchRequest(pattern, text, scope, doubleHash, null, null);
    }

    public static FuzzyRequest fuzzy(Map<String, Object> input) {
        String query = str(input, "query");
        if (query == null || query.isBlank()) {
            throw new InvalidQueryException("query is required");
        }
        Integer maxDistance = integer(input, "maxDistance", 2);
        if (maxDistance < 0 || maxDistance > 8) {
            throw new InvalidQueryException("maxDistance must be in [0, 8]");
        }
        return new FuzzyRequest(boundedText(input, "text"), query, maxDistance);
    }

    public static DistanceRequest edit(Map<String, Object> input, boolean showMatrix) {
        return new DistanceRequest(requiredText(input, "a"), requiredText(input, "b"),
                showMatrix);
    }

    public static AlignmentRequest alignment(Map<String, Object> input) {
        Integer match = integer(input, "match", 1);
        Integer mismatch = integer(input, "mismatch", -1);
        Integer gap = integer(input, "gap", -1);
        return new AlignmentRequest(null, null, requiredText(input, "a"), requiredText(input, "b"),
                match, mismatch, gap);
    }

    public static MatrixChainRequest matrixChain(Map<String, Object> input) {
        int[] dims = longs(input, "dims").stream().mapToInt(Long::intValue).toArray();
        if (dims.length < 2 || dims.length > MAX_INTERVAL_DIMS) {
            throw new InvalidQueryException("dims must contain between 2 and " + MAX_INTERVAL_DIMS
                    + " positive dimension entries (interval DP is O(n^3))");
        }
        return new MatrixChainRequest(dims);
    }

    public static QuicksortRequest quicksort(Map<String, Object> input) {
        long[] values = longs(input, "values").stream().mapToLong(Long::longValue).toArray();
        return new QuicksortRequest(values, longOrNull(input, "seed"));
    }

    public static ReservoirRequest reservoir(Map<String, Object> input) {
        Integer k = integer(input, "k", 10);
        Integer size = integer(input, "size", 0);
        long[] values = longs(input, "values").stream().mapToLong(Long::longValue).toArray();
        return new ReservoirRequest(k, size, values);
    }

    public static MillerRabinRequest millerRabin(Map<String, Object> input) {
        Long n = longOrNull(input, "n");
        if (n == null || n < 0) {
            throw new InvalidQueryException("n (the number under test) is required");
        }
        return new MillerRabinRequest(n, integer(input, "rounds", null));
    }

    public static FlowRequest flow(Map<String, Object> input) {
        String source = str(input, "source");
        String sink = str(input, "sink");
        if (source == null || sink == null) {
            throw new InvalidQueryException("source and sink are required");
        }
        List<String> nodes = strings(input, "nodes");
        GraphEdge[] edges = graphEdges(input);
        if (nodes.isEmpty() || edges.length == 0) {
            throw new InvalidQueryException("nodes and edges are required");
        }
        return new FlowRequest(source, sink, nodes.toArray(new String[0]), edges);
    }

    public static VertexCoverRequest vertexCover(Map<String, Object> input) {
        List<String> nodes = strings(input, "nodes");
        List<String[]> edges = stringPairs(input, "edges");
        if (nodes.isEmpty()) {
            throw new InvalidQueryException("nodes are required");
        }
        return new VertexCoverRequest(nodes.toArray(new String[0]),
                edges.toArray(new String[0][0]));
    }

    public static Long seedOf(Map<String, Object> input) {
        return longOrNull(input, "seed");
    }

    // ------------------------------------------------------------------ helpers

    private static Scope scope(Map<String, Object> input) {
        String value = str(input, "scope");
        if (value == null || value.isBlank()) {
            return Scope.EXPLICIT;
        }
        try {
            return Scope.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidQueryException("scope must be EXPLICIT or DATASET");
        }
    }

    private static String requiredText(Map<String, Object> input, String key) {
        String value = boundedText(input, key);
        if (value == null || value.isBlank()) {
            throw new InvalidQueryException(key + " is required");
        }
        return value;
    }

    private static String boundedText(Map<String, Object> input, String key) {
        String value = str(input, key);
        if (value != null && value.length() > MAX_TEXT_LENGTH) {
            throw new InvalidQueryException(key + " exceeds " + MAX_TEXT_LENGTH + " characters");
        }
        return value;
    }

    private static List<String> strings(Map<String, Object> input, String key) {
        Object raw = input.get(key);
        if (raw instanceof List<?> list) {
            if (list.size() > MAX_ARRAY_ELEMENTS) {
                throw new InvalidQueryException(key + " exceeds " + MAX_ARRAY_ELEMENTS + " entries");
            }
            return list.stream().map(String::valueOf).toList();
        }
        if (raw instanceof String[] array) {
            if (array.length > MAX_ARRAY_ELEMENTS) {
                throw new InvalidQueryException(key + " exceeds " + MAX_ARRAY_ELEMENTS + " entries");
            }
            return List.of(array);
        }
        return List.of();
    }

    private static List<String[]> stringPairs(Map<String, Object> input, String key) {
        Object raw = input.get(key);
        if (raw instanceof List<?> list) {
            if (list.size() > MAX_ARRAY_ELEMENTS) {
                throw new InvalidQueryException(key + " exceeds " + MAX_ARRAY_ELEMENTS + " edges");
            }
            return list.stream()
                    .map(item -> {
                        @SuppressWarnings("unchecked")
                        List<Object> pair = (List<Object>) item;
                        return new String[]{String.valueOf(pair.get(0)),
                                String.valueOf(pair.get(1))};
                    })
                    .toList();
        }
        if (raw instanceof String[][] array) {
            if (array.length > MAX_ARRAY_ELEMENTS) {
                throw new InvalidQueryException(key + " exceeds " + MAX_ARRAY_ELEMENTS + " edges");
            }
            return List.of(array);
        }
        return List.of();
    }

    private static GraphEdge[] graphEdges(Map<String, Object> input) {
        Object raw = input.get("edges");
        if (raw instanceof GraphEdge[] edges) {
            return edges;
        }
        if (raw instanceof List<?> list) {
            if (list.size() > MAX_ARRAY_ELEMENTS) {
                throw new InvalidQueryException("edges exceeds " + MAX_ARRAY_ELEMENTS + " entries");
            }
            return list.stream()
                    .map(item -> {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> e = (Map<String, Object>) item;
                        return new GraphEdge(str(e, "from"), str(e, "to"),
                                longOrNull(e, "capacity"), longOrNull(e, "cost"));
                    })
                    .toArray(GraphEdge[]::new);
        }
        throw new InvalidQueryException("edges must be a list of {from, to, capacity?}");
    }

    private static List<Long> longs(Map<String, Object> input, String key) {
        Object raw = input.get(key);
        if (raw instanceof List<?> list) {
            if (list.size() > MAX_ARRAY_ELEMENTS) {
                throw new InvalidQueryException(key + " exceeds " + MAX_ARRAY_ELEMENTS + " entries");
            }
            return list.stream().map(v -> ((Number) v).longValue()).toList();
        }
        if (raw instanceof long[] array) {
            if (array.length > MAX_ARRAY_ELEMENTS) {
                throw new InvalidQueryException(key + " exceeds " + MAX_ARRAY_ELEMENTS + " entries");
            }
            List<Long> out = new java.util.ArrayList<>(array.length);
            for (long v : array) {
                out.add(v);
            }
            return out;
        }
        if (raw instanceof Number[] array) {
            if (array.length > MAX_ARRAY_ELEMENTS) {
                throw new InvalidQueryException(key + " exceeds " + MAX_ARRAY_ELEMENTS + " entries");
            }
            return java.util.Arrays.stream(array).map(Number::longValue).toList();
        }
        return List.of();
    }

    private static String str(Map<String, Object> input, String key) {
        Object value = input.get(key);
        if (value == null) {
            return null;
        }
        return value instanceof String text ? text : String.valueOf(value);
    }

    private static Boolean bool(Map<String, Object> input, String key) {
        Object value = input.get(key);
        if (value == null) {
            return null;
        }
        return value instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(value));
    }

    private static Integer integer(Map<String, Object> input, String key, Integer fallback) {
        Object value = input.get(key);
        if (value == null) {
            return fallback;
        }
        return ((Number) value).intValue();
    }

    private static Long longOrNull(Map<String, Object> input, String key) {
        Object value = input.get(key);
        if (value == null) {
            return null;
        }
        return value instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(value));
    }
}