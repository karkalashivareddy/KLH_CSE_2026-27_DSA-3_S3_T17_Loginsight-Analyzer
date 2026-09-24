package com.loginsight.dto.request;

import java.time.Instant;

/**
 * Body of the product search endpoints (docs/API.md §3): the LogInsight query string plus optional
 * time-window and paging controls. Defaults: page 1, size 25, sort {@code timestamp:desc}.
 */
public record LogSearchRequest(String query, Instant from, Instant to, Integer page, Integer size,
                               String sort) {

    public LogSearchRequest {
        page = page == null ? 1 : page;
        size = size == null ? 25 : size;
        sort = sort == null ? "timestamp:desc" : sort;
        if (page < 1) {
            throw new IllegalArgumentException("page must be >= 1");
        }
        if (size < 1 || size > 200) {
            throw new IllegalArgumentException("size must be between 1 and 200");
        }
    }
}