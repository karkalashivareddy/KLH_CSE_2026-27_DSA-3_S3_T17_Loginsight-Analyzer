package com.loginsight.dto.request;

/**
 * Body of {@code POST /api/random/prime} (docs/12 §5). Checks {@code n} with Miller-Rabin over
 * {@code rounds} random bases (default 20). Validation rejects {@code n < 2}.
 */
public record MillerRabinRequest(Long n, Integer rounds) {

    public MillerRabinRequest {
        rounds = rounds == null ? 20 : rounds;
    }
}