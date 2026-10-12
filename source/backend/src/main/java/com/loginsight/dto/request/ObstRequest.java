package com.loginsight.dto.request;

/**
 * Body of {@code POST /api/dp/obst} (docs/12 §4).
 */
public record ObstRequest(long[] freqs) {
}