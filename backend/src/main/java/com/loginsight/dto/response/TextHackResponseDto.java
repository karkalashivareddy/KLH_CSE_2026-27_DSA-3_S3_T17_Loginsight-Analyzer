package com.loginsight.dto.response;

import java.util.List;

import com.loginsight.catalog.AlgorithmInfo;

/**
 * TextHack unified-query response (docs/REBUILD_BASELINE Phase-3). Carries the human label of the
 * query class, the module framing, the recommended drill-down algorithms and the {@code executed}
 * canonical envelope produced by the real engine. {@code traceAlgorithmKey} is the catalogue key of
 * the trace-instrumented algorithm with the same input, so the lab can deep-link to a full trace.
 */
public record TextHackResponseDto(String queryClass, String label, String moduleId,
                                  String moduleLabel, String description,
                                  List<AlgorithmInfo> recommended, AlgorithmResultDto executed,
                                  String traceAlgorithmKey) {
}