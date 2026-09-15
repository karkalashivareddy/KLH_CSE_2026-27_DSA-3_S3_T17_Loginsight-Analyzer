package com.loginsight.dto.request;

/**
 * Body of {@code POST /api/dp/matrix-chain} (docs/12 §4).
 */
public record MatrixChainRequest(int[] dims) {
}