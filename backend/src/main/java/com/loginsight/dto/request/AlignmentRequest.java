package com.loginsight.dto.request;

/**
 * Body of the sequence-alignment endpoints (docs/12 §4). Supports token-sequence alignment
 * ({@code seqA}/{@code seqB}, typically service names) and single-string alignment
 * ({@code a}/{@code b}) with scoring defaults {@code match +1}, {@code mismatch -1}, {@code gap -1}
 * unless overridden.
 */
public record AlignmentRequest(String[] seqA, String[] seqB, String a, String b, Integer match,
                               Integer mismatch, Integer gap) {

    public AlignmentRequest {
        match = match == null ? 1 : match;
        mismatch = mismatch == null ? -1 : mismatch;
        gap = gap == null ? -1 : gap;
    }
}