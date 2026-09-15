package com.loginsight.dto.request;

/**
 * Body of {@code POST /api/dp/hamiltonian} (docs/12 §4). {@code from[i]}/{@code to[i]} are undirected
 * edges; {@code start} defaults to vertex 0. The vertex count is derived from the trailing edge
 * endpoints unless it exceeds them.
 */
public record HamiltonianRequest(int[] from, int[] to, Integer start) {

    public HamiltonianRequest {
        start = start == null ? 0 : start;
    }
}