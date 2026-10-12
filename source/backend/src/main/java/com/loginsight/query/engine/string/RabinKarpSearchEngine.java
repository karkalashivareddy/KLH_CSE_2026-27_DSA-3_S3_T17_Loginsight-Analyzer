package com.loginsight.query.engine.string;

import java.util.HashMap;
import java.util.Map;

import com.loginsight.dsa.string.RabinKarpMatcher;
import com.loginsight.dsa.string.StringSearchResult;
import com.loginsight.dto.request.SearchRequest;
import com.loginsight.model.AlgorithmType;
import com.loginsight.model.QueryResult;
import com.loginsight.model.QueryType;
import com.loginsight.query.QueryContext;
import com.loginsight.query.QueryEngine;
import com.loginsight.query.QueryValidator;
import com.loginsight.query.Results;

/**
 * Rabin-Karp rolling-hash matcher (docs/12 §1). With default knobs it delegates to the shared
 * double-hash engine; when the caller supplies a custom {@code base}/{@code prime} the engine runs
 * its own single-modulus rolling hash with the supplied prime and character-by-character
 * verification of every hash collision, so false positives are never reported.
 */
public final class RabinKarpSearchEngine implements QueryEngine {

    private static final long DEFAULT_BASE = 911_382_323L;
    private static final long DEFAULT_MOD = 1_000_000_007L;

    @Override
    public QueryType type() {
        return QueryType.PATTERN_SEARCH;
    }

    @Override
    public AlgorithmType algorithm() {
        return AlgorithmType.RABIN_KARP;
    }

    @Override
    public QueryResult execute(QueryContext context) {
        QueryValidator.requirePattern(context.getPattern());
        SearchRequest request = (SearchRequest) context.getRequest();
        String text = context.getText();
        String pattern = context.getPattern();
        int n = text.length();
        int m = pattern.length();

        boolean custom = request.prime() != null && request.prime() > 0;
        if (!custom) {
            StringSearchResult matched = new RabinKarpMatcher()
                    .match(text, pattern, Boolean.TRUE.equals(request.doubleHash()));
            Map<String, Object> result = Map.of("matchCount", matched.getMatchCount(),
                    "positions", matched.getMatchPositions());
            Object intermediate = Boolean.TRUE.equals(request.doubleHash())
                    ? "double hash (mod 1e9+7, mod 1e9+9)"
                    : "single hash (mod 1e9+7)";
            return Results.timed(type(), algorithm(), n, matched.getExecutionTimeNanos(), result,
                    intermediate, 0L, matched.getTimeComplexity(), matched.getSpaceComplexity(),
                    "rolling hash " + intermediate + " with collision verification");
        }

        long base = request.base() != null && request.base() > 0 ? request.base() : DEFAULT_BASE;
        long prime = request.prime();
        int[] positions = customSearch(text.toCharArray(), pattern.toCharArray(), base, prime);
        Map<String, Object> result = Map.of("matchCount", positions.length, "positions", positions);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("base", base);
        parameters.put("prime", prime);
        parameters.put("doubleHash", Boolean.FALSE.equals(request.doubleHash()) ? false : request.doubleHash());
        return Results.timed(type(), algorithm(), n, 0L, result, parameters, 0L,
                "O(n + m) avg, O(n·m) worst", "O(1) auxiliary",
                "custom rolling hash with explicit collision verification");
    }

    private static int[] customSearch(char[] t, char[] p, long base, long prime) {
        int n = t.length;
        int m = p.length;
        if (m > n) {
            return new int[0];
        }
        long patternHash = 0;
        long windowHash = 0;
        long power = 1;
        for (int i = 0; i < m; i++) {
            patternHash = (patternHash * base + p[i]) % prime;
            windowHash = (windowHash * base + t[i]) % prime;
            if (i < m - 1) {
                power = (power * base) % prime;
            }
        }
        int[] positions = new int[n - m + 1];
        int count = 0;
        for (int i = 0; i <= n - m; i++) {
            if (patternHash == windowHash && matchesAt(t, p, i)) {
                positions[count++] = i;
            }
            if (i < n - m) {
                windowHash = ((windowHash - power * t[i] % prime + prime) * base + t[i + m]) % prime;
            }
        }
        int[] trimmed = new int[count];
        System.arraycopy(positions, 0, trimmed, 0, count);
        return trimmed;
    }

    private static boolean matchesAt(char[] t, char[] p, int offset) {
        for (int j = 0; j < p.length; j++) {
            if (t[offset + j] != p[j]) {
                return false;
            }
        }
        return true;
    }
}