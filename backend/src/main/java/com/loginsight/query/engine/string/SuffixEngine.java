package com.loginsight.query.engine.string;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.loginsight.dsa.string.suffix.KasaiLCP;
import com.loginsight.dsa.string.suffix.SuffixArray;
import com.loginsight.exception.InvalidQueryException;
import com.loginsight.model.AlgorithmType;
import com.loginsight.model.QueryResult;
import com.loginsight.model.QueryType;
import com.loginsight.query.QueryContext;
import com.loginsight.query.QueryEngine;
import com.loginsight.query.QueryValidator;
import com.loginsight.query.Results;

/**
 * Suffix-array scenario with two modes selected by the {@code variant} parameter (docs/12 §1):
 * <ul>
 * <li>{@code build} — constructs the sorted suffix array and reports the lexicographic head plus
 *     the LCP array (Kasai) as intermediate evidence;</li>
 * <li>{@code search} — binary-searches the pattern in O(m log n) over a fresh build and reports the
 *     start positions, with the LCP array again exposed.</li>
 * </ul>
 */
public final class SuffixEngine implements QueryEngine {

    @Override
    public QueryType type() {
        return QueryType.SUFFIX_ANALYSIS;
    }

    @Override
    public AlgorithmType algorithm() {
        return AlgorithmType.SUFFIX_ARRAY;
    }

    @Override
    public QueryResult execute(QueryContext context) {
        String variant = (String) context.getParam("variant");
        if (variant == null) {
            throw new InvalidQueryException("variant must be 'build' or 'search'");
        }
        String text = context.getText();
        QueryValidator.requireText(text);

        if ("build".equals(variant)) {
            return build(text);
        }
        if ("search".equals(variant)) {
            QueryValidator.requirePattern(context.getPattern());
            return search(text, context.getPattern());
        }
        throw new InvalidQueryException("variant must be 'build' or 'search', got '" + variant + "'");
    }

    private QueryResult build(String text) {
        long start = System.nanoTime();
        SuffixArray suffixArray = SuffixArray.build(text);
        int[] lcp = KasaiLCP.build(text.toCharArray(), suffixArray.getSuffixArray());
        long elapsed = System.nanoTime() - start;

        int[] head = new int[Math.min(8, suffixArray.length())];
        for (int i = 0; i < head.length; i++) {
            head[i] = suffixArray.getSuffixArray()[i];
        }
        List<String> sampleSuffixes = new ArrayList<>();
        for (int i = 0; i < head.length; i++) {
            sampleSuffixes.add(suffixArray.suffixAt(head[i]));
        }
        Map<String, Object> result = Map.of("length", suffixArray.length(),
                "headPositions", head, "sampleSuffixes", sampleSuffixes);
        return Results.measured(type(), algorithm(), text.length(), start, result, lcp,
                8L * suffixArray.length(), "O(n log n) build", "O(n) suffix array + LCP",
                "prefix-doubling (or equivalent) build with Kasai LCP");
    }

    private QueryResult search(String text, String pattern) {
        long start = System.nanoTime();
        SuffixArray suffixArray = SuffixArray.build(text);
        int[] positions = suffixArray.search(pattern);
        int[] lcp = KasaiLCP.build(text.toCharArray(), suffixArray.getSuffixArray());
        long elapsed = System.nanoTime() - start;

        Map<String, Object> result = Map.of("matchCount", positions.length, "positions", positions);
        return Results.measured(type(), algorithm(), text.length(), start, result, lcp,
                8L * suffixArray.length(), "O(n log n) build + O(m log n) search",
                "O(n) auxiliary", "binary search over the sorted suffix array with LCP on the side");
    }
}