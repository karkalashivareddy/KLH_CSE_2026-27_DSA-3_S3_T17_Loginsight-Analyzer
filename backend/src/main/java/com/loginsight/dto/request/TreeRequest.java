package com.loginsight.dto.request;

/**
 * Body of {@code POST /api/dp/tree} (docs/12 §4). Undirected tree given as {@code from[i]}/{@code to[i]}
 * edges; the vertex count is {@code max(endpoint)+1} unless the caller passes {@code vertexCount}.
 */
public record TreeRequest(int[] from, int[] to, Integer vertexCount) {

    public TreeRequest {
        vertexCount = vertexCount == null ? 0 : vertexCount;
    }
}