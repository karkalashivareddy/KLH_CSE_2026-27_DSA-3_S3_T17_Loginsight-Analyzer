package com.loginsight.simulation;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Sliding-window error-burst detection.
 *
 * <p>Events are aggregated into fixed-width simulated buckets. For the bucket currently closing, the
 * detector compares its ERROR/FATAL count against the mean of the preceding baseline buckets and
 * reports the ratio. This is the "error burst detection" role the sliding window plays in LogInsight:
 * a single high bucket is noise, a sustained ratio above the threshold is a signal.</p>
 *
 * <p>State is a fixed-size deque, so memory is bounded regardless of stream length.</p>
 */
public final class BurstDetector {

    /** Simulated milliseconds per bucket. */
    public static final int BUCKET_MILLIS = 1_000;
    /** Number of trailing buckets kept for the baseline mean. */
    public static final int BASELINE_BUCKETS = 12;
    /** Ratio of current-bucket errors to baseline mean that constitutes a burst. */
    public static final double BURST_RATIO = 2.2;
    /** Absolute floor so an idle baseline cannot make a single error look like a burst. */
    public static final int MIN_ERRORS = 4;

    private final Deque<int[]> buckets = new ArrayDeque<>();
    private int currentBucket = -1;
    private int currentTickIndex = Integer.MIN_VALUE;

    /** Result of one bucket evaluation. */
    public record Reading(
            int bucket,
            int currentErrors,
            double baselineMean,
            double ratio,
            int currentTotal,
            boolean burst) {
    }

    /**
     * Feeds one event into the window.
     *
     * @param tick       the simulation tick the event belongs to
     * @param isError    whether the event is ERROR or FATAL
     * @return a reading when the event closes a bucket, otherwise {@code null}
     */
    public Reading observe(int tick, boolean isError) {
        int bucket = Math.toIntExact((long) tick * ScenarioEventFactory.TICK_MILLIS / BUCKET_MILLIS);
        if (bucket != currentBucket) {
            Reading previous = currentBucket < 0 ? null : close(currentBucket);
            currentBucket = bucket;
            currentTickIndex = tick;
            buckets.addLast(new int[] { bucket, 0, 0 });
            while (buckets.size() > BASELINE_BUCKETS + 1) {
                buckets.removeFirst();
            }
            if (previous != null) {
                return previous;
            }
        }
        int[] slot = buckets.peekLast();
        if (slot == null) {
            return null;
        }
        slot[2]++;
        if (isError) {
            slot[1]++;
        }
        return null;
    }

    /** Closes the in-flight bucket and evaluates it. */
    public Reading flush() {
        if (currentBucket < 0) {
            return null;
        }
        return close(currentBucket);
    }

    private Reading close(int bucket) {
        int[] current = buckets.peekLast();
        if (current == null || current[0] != bucket) {
            return null;
        }
        double baselineMean = 0.0;
        int samples = 0;
        var iterator = buckets.descendingIterator();
        iterator.next();
        while (iterator.hasNext()) {
            baselineMean += iterator.next()[1];
            samples++;
        }
        if (samples > 0) {
            baselineMean /= samples;
        }
        double ratio = baselineMean <= 0.0 ? (current[1] > 0 ? Double.POSITIVE_INFINITY : 0.0)
                : current[1] / baselineMean;
        boolean burst = current[1] >= MIN_ERRORS && ratio >= BURST_RATIO;
        return new Reading(bucket, current[1], baselineMean, ratio, current[2], burst);
    }

    /** Resets the window, used when a scenario is restarted. */
    public void reset() {
        buckets.clear();
        currentBucket = -1;
        currentTickIndex = Integer.MIN_VALUE;
    }

    /** Bucket index currently being accumulated. */
    public int currentBucket() {
        return currentBucket;
    }

    /** Tick index currently being accumulated. */
    public int currentTick() {
        return currentTickIndex;
    }
}
