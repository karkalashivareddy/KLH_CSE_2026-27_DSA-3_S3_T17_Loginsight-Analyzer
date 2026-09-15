package com.loginsight.query.engine.randomized;

import java.util.Map;

import com.loginsight.dsa.randomized.MillerRabin;
import com.loginsight.dsa.randomized.MillerRabinResult;
import com.loginsight.dsa.randomized.RandomSource;
import com.loginsight.dto.request.MillerRabinRequest;
import com.loginsight.model.AlgorithmType;
import com.loginsight.model.QueryResult;
import com.loginsight.model.QueryType;
import com.loginsight.query.QueryContext;
import com.loginsight.query.QueryEngine;
import com.loginsight.query.QueryValidator;
import com.loginsight.query.Results;

/**
 * Miller-Rabin primality check (docs/12 §5). Small numbers and trivial even cases are decided
 * deterministically; composites use {@code rounds} random bases drawn from a seeded source so the
 * same request reproduces the same witnesses. The witness bases and mode are part of the payload.
 */
public final class MillerRabinEngine implements QueryEngine {

    @Override
    public QueryType type() {
        return QueryType.PRIMALITY_TEST;
    }

    @Override
    public AlgorithmType algorithm() {
        return AlgorithmType.MILLER_RABIN;
    }

    @Override
    public QueryResult execute(QueryContext context) {
        MillerRabinRequest request = (MillerRabinRequest) context.getRequest();
        long n = QueryValidator.requireMillerRabin(request);
        long seed = context.getParam("seed") instanceof Long s ? s : 0x5EED;

        long start = System.nanoTime();
        RandomSource rng = RandomSource.seeded(seed);
        MillerRabinResult result = new MillerRabin().isProbablePrime(n, request.rounds(), rng);
        long elapsed = System.nanoTime() - start;

        Map<String, Object> payload = Map.of("n", result.getN(), "isPrime", result.isPrime(),
                "mode", result.getMode(), "rounds", result.getRounds(),
                "witnesses", result.getWitnesses());
        return Results.measured(type(), algorithm(), (int) Math.min(n, 1_000_000), start, payload,
                result.getNote(), 8L * result.getWitnesses().length,
                "O(rounds · log³ n)", "O(1) auxiliary",
                result.getNote());
    }
}