package com.loginsight.dsa.parallel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Package-private execution substrate for the parallel engine.
 *
 * <p>Every top-level parallel call creates a fresh {@link ExecutorService} with exactly the
 * requested worker count and shuts it down in a {@code finally} block.  There is no shared,
 * long-lived global pool, so tests cannot interfere with each other and the worker count is
 * deterministic per call.  Worker threads are daemons so a forgotten shut-down can never hang
 * the JVM.</p>
 *
 * <p>This helper is deliberately package-private: the public contract of
 * {@code com.loginsight.dsa.parallel} is the engine classes listed in {@code docs/03}.</p>
 */
final class ParallelSupport {

    /** Parallel execution cuts in only above this size; smaller inputs run sequentially. */
    static final int SEQUENTIAL_THRESHOLD = 1024;

    private ParallelSupport() {
    }

    /** Creates a daemon-thread pool with exactly {@code parallelism} workers. */
    static ExecutorService newPool(int parallelism) {
        return Executors.newFixedThreadPool(parallelism, new ThreadFactory() {
            private final AtomicInteger serial = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "loginsight-parallel-" + serial.getAndIncrement());
                t.setDaemon(true);
                return t;
            }
        });
    }

    static int ceilDiv(int a, int b) {
        return (a + b - 1) / b;
    }

    static void requireParallelism(int parallelism) {
        if (parallelism < 1) {
            throw new IllegalArgumentException("parallelism must be >= 1, got " + parallelism);
        }
    }
}