package com.loginsight.dto.request;

/**
 * Body of {@code POST /api/dp/sos} (docs/12 §4). {@code values} must hold exactly {@code 2^bits}
 * elements; {@code bits} defaults to {@code log2(values.length)} when omitted.
 */
public record SosRequest(long[] values, Integer bits) {

    public SosRequest {
        if (bits == null && values != null) {
            int size = values.length;
            if (size > 0 && (size & (size - 1)) == 0) {
                bits = Math.max(0, Integer.numberOfTrailingZeros(size));
            } else {
                bits = 0;
            }
        }
        bits = bits == null ? 0 : bits;
    }
}