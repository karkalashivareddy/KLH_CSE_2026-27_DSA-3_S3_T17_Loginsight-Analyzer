package com.loginsight.query.engine.randomized;

import java.util.Map;

import com.loginsight.dsa.randomized.RandomSource;
import com.loginsight.dsa.randomized.ReservoirSampling;
import com.loginsight.dto.request.ReservoirRequest;
import com.loginsight.model.AlgorithmType;
import com.loginsight.model.QueryResult;
import com.loginsight.model.QueryType;
import com.loginsight.query.QueryContext;
import com.loginsight.query.QueryEngine;
import com.loginsight.query.QueryValidator;
import com.loginsight.query.Results;

/**
 * One-pass reservoir sampling (docs/12 §5): exactly {@code k} elements are drawn from a stream with
 * probability {@code k / n} and uniform replacement, in a single pass over O(k) memory. When
 * {@code values} is supplied it becomes the stream; otherwise the stream is {@code [0, size)}.
 */
public final class ReservoirEngine implements QueryEngine {

    @Override
    public QueryType type() {
        return QueryType.STREAM_SAMPLE;
    }

    @Override
    public AlgorithmType algorithm() {
        return AlgorithmType.RESERVOIR_SAMPLING;
    }

    @Override
    public QueryResult execute(QueryContext context) {
        ReservoirRequest request = (ReservoirRequest) context.getRequest();
        long[] stream = context.getValues();
        long streamSize = stream != null ? stream.length : request.size();
        QueryValidator.requireReservoir(request, streamSize);
        int k = request.k();
        long seed = context.getParam("seed") instanceof Long s ? s : 42L;

        long start = System.nanoTime();
        ReservoirSampling reservoir = new ReservoirSampling(k, RandomSource.seeded(seed));
        if (stream != null) {
            for (long value : stream) {
                reservoir.offer(value);
            }
        } else {
            for (int i = 0; i < streamSize; i++) {
                reservoir.offer(i);
            }
        }
        long[] sample = reservoir.sample();
        long elapsed = System.nanoTime() - start;

        Map<String, Object> payload = Map.of("k", reservoir.getK(),
                "seen", reservoir.countSeen(), "sample", sample);
        return Results.measured(type(), algorithm(), (int) streamSize, start, payload,
                Map.of("replacementRule", "j-th element admitted with probability k/j (0-indexed)"),
                8L * k, "O(n) stream, O(k) memory", "O(k) reservoir",
                "uniform random k-subset, one pass, seeded for reproducibility");
    }
}