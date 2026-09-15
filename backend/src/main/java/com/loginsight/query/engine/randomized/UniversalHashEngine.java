package com.loginsight.query.engine.randomized;

import java.util.Map;

import com.loginsight.dsa.randomized.RandomSource;
import com.loginsight.dsa.randomized.UniversalHashFamily;
import com.loginsight.dto.request.HashRequest;
import com.loginsight.model.AlgorithmType;
import com.loginsight.model.QueryResult;
import com.loginsight.model.QueryType;
import com.loginsight.query.QueryContext;
import com.loginsight.query.QueryEngine;
import com.loginsight.query.QueryValidator;
import com.loginsight.query.Results;

/**
 * Universal hashing of a log-snippet string (docs/12 §5). The text is reduced to a long key by a
 * polynomial rolling hash, then hashed through a randomly-drawn universal family into a table of
 * size {@code m}; the drawn family coefficients ({@code a}, {@code b}), the Mersenne prime modulus
 * and the resulting bucket are the payload.
 */
public final class UniversalHashEngine implements QueryEngine {

    private static final long POLY_BASE = 131L;

    @Override
    public QueryType type() {
        return QueryType.HASH;
    }

    @Override
    public AlgorithmType algorithm() {
        return AlgorithmType.UNIVERSAL_HASH;
    }

    @Override
    public QueryResult execute(QueryContext context) {
        HashRequest request = (HashRequest) context.getRequest();
        String text = QueryValidator.requireNotBlank(request.text(), "text");
        int m = request.m();
        QueryValidator.requireBounds(2, m, 1_000_000, "m");
        long seed = request.seed();

        long start = System.nanoTime();
        long key = polynomialKey(text, UniversalHashFamily.DEFAULT_PRIME);
        RandomSource rng = RandomSource.seeded(seed);
        UniversalHashFamily family = UniversalHashFamily.random(UniversalHashFamily.DEFAULT_PRIME, m, rng);
        int bucket = family.hash(key);
        long elapsed = System.nanoTime() - start;

        Map<String, Object> payload = Map.of("textKey", key, "hashValue", bucket,
                "tableSize", family.getTableSize(), "a", family.getA(), "b", family.getB(),
                "prime", family.getPrime());
        return Results.measured(type(), algorithm(), text.length(), start, payload,
                Map.of("polynomialBase", POLY_BASE), 0L, "O(|text| + 1) hashing",
                "O(1) family state", "family drawn per request with seed " + seed);
    }

    private static long polynomialKey(String text, long mod) {
        long key = 0;
        for (int i = 0; i < text.length(); i++) {
            key = (key * POLY_BASE + text.charAt(i)) % mod;
        }
        return key;
    }
}