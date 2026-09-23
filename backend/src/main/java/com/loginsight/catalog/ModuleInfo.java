package com.loginsight.catalog;

import java.util.List;

/**
 * Module metadata for the Algorithm Laboratory sidebar and course map (docs/REBUILD_BASELINE
 * Phase-2 <em>course map / module framing</em>). Counts are computed from the real catalogue, never
 * hard-coded, so they cannot drift from the algorithm list.
 *
 * @param id             stable module id
 * @param label          short sidebar label
 * @param title          long module title
 * @param description    DSA-3 scope statement for the module
 * @param accent         CSS accent for the module (design-token hex)
 * @param algorithmCount total implemented algorithms in the module
 * @param exposedCount   algorithms reachable through at least one REST endpoint
 * @param trackableCount trace-instrumented algorithms in the module
 * @param algorithms     ordered algorithm descriptors
 */
public record ModuleInfo(String id, String label, String title, String description, String accent,
                         int algorithmCount, int exposedCount, int trackableCount,
                         List<AlgorithmInfo> algorithms) {
}