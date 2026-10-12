package com.loginsight.dto.request;

/**
 * Body of {@code POST /api/random/hash} (docs/12 §5). Hashes {@code text} through a universal family
 * into a table of size {@code m} (default 64) with a seeded family (default {@code 1337}). The
 * engine returns the hash value and the family coefficients actually drawn.
 */
public record HashRequest(String text, Long seed, Integer m) {

    public HashRequest {
        seed = seed == null ? 1337L : seed;
        m = m == null ? 64 : m;
    }
}