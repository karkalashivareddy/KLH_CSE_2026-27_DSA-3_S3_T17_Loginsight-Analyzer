package com.loginsight.run;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * Bounded in-memory run history (docs/REBUILD_BASELINE Phase-4). Keeps the {@code maxRuns} most
 * recent completed runs with thread-safe access; older runs are evicted FIFO. Deliberately
 * in-memory: history is diagnostic and resets with the process - the docs state this honestly.
 */
@Component
public class RunStore {

    private static final int MAX_RUNS = 64;

    private final Map<String, RunRecord> byId = new LinkedHashMap<>(
            1 + (int) (MAX_RUNS / 0.75f), 0.75f, true);

    public synchronized void put(RunRecord run) {
        byId.put(run.runId(), run);
        List<String> oldest = new ArrayList<>();
        for (String id : byId.keySet()) {
            if (byId.size() - oldest.size() <= MAX_RUNS) {
                break;
            }
            oldest.add(id);
        }
        for (String id : oldest) {
            byId.remove(id);
        }
    }

    public synchronized RunRecord get(String runId) {
        return byId.get(runId);
    }

    /** Newest-run-first summaries of all retained runs. */
    public synchronized List<RunRecord> newestFirst() {
        List<RunRecord> out = new ArrayList<>(byId.values());
        Collections.reverse(out);
        return List.copyOf(out);
    }
}