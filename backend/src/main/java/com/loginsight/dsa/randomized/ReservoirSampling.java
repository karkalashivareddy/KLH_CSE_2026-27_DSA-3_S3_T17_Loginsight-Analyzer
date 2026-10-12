package com.loginsight.dsa.randomized;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.loginsight.trace.StepRecorder;
import com.loginsight.trace.TracedResult;

/**
 * Reservoir sampling — Algorithm R (Vitter, 1985).
 *
 * <p>Maintains a uniform random sample of size {@code k} from a stream of unknown length
 * seen one element at a time via {@link #offer(long)}.  Only O(k) memory is used regardless
 * of the stream length.</p>
 *
 * <h2>Contract</h2>
 * <ul>
 *   <li>{@code k = 0}: the sample is always empty; {@code offer} is a no-op.</li>
 *   <li>{@code k ≥ stream length}: the sample contains every element of the stream.</li>
 *   <li>{@code 0 < k < stream length}: at any point, each of the {@code countSeen()} elements
 *       seen so far is equally likely to be in the sample.</li>
 * </ul>
 *
 * <h2>Complexity</h2>
 * <ul>
 *   <li>{@link #offer} — O(1) amortised.</li>
 *   <li>{@link #sample} — O(k) to copy.</li>
 * </ul>
 *
 * <p>No {@code java.util.Arrays} or collections are used; the reservoir is a plain
 * {@code long[]}.</p>
 */
public final class ReservoirSampling {

    private final int k;
    private final RandomSource rng;
    private final long[] reservoir;
    private int count;

    /**
     * @param k   reservoir capacity (must be &ge; 0)
     * @param rng random source (must not be null when {@code k > 0})
     */
    public ReservoirSampling(int k, RandomSource rng) {
        if (k < 0) {
            throw new IllegalArgumentException("k must be >= 0, got " + k);
        }
        this.k = k;
        this.rng = rng;
        this.reservoir = new long[k];
        this.count = 0;
    }

    /**
     * Offers the next element of the stream to the reservoir.
     *
     * <p>Algorithm R: for the {@code i}-th element (1-based), if {@code i ≤ k} the element
     * is placed directly; otherwise it replaces a random existing element with probability
     * {@code k / i}.</p>
     *
     * @param item the next stream element
     */
    public void offer(long item) {
        count++;
        if (count <= k) {
            reservoir[count - 1] = item;
        } else {
            int j = rng.nextInt(count);
            if (j < k) {
                reservoir[j] = item;
            }
        }
    }

    /**
     * Returns a defensive copy of the current reservoir.
     *
     * <p>If fewer than {@code k} elements have been offered the returned array has length
     * equal to the number of elements seen so far.</p>
     */
    public long[] sample() {
        int len = Math.min(count, k);
        long[] result = new long[len];
        System.arraycopy(reservoir, 0, result, 0, len);
        return result;
    }

    /** Reservoir capacity. */
    public int getK() {
        return k;
    }

    /** Total number of elements offered to the reservoir so far. */
    public int countSeen() {
        return count;
    }

    /**
     * Trace-capable path: identical Algorithm R loop over the provided stream, recording a step per
     * offered element (position, decision: direct fill / replace / skip, chosen j, reservoir state).
     */
    public TracedResult sampleTracked(long[] stream) {
        StepRecorder recorder = new StepRecorder();
        long start = System.nanoTime();
        long[] reservoir = new long[k];
        int countSeen = 0;
        List<Long> streamList = new ArrayList<>();
        for (long item : stream) {
            countSeen++;
            streamList.add(item);
            String decision;
            Map<String, Object> state;
            if (countSeen <= k) {
                reservoir[countSeen - 1] = item;
                decision = "FILL";
                state = StepRecorder.state("phase", "sample", "i", countSeen, "k", k, "item",
                        item, "decision", decision, "reservoir", box(reservoir,
                                Math.min(countSeen, k)), "stream", streamList);
            } else {
                int j = rng.nextInt(countSeen);
                decision = j < k ? "REPLACE" : "SKIP";
                if (j < k) {
                    reservoir[j] = item;
                }
                state = StepRecorder.state("phase", "sample", "i", countSeen, "k", k, "item",
                        item, "decision", decision, "j", j, "reservoir", box(reservoir, k),
                        "stream", streamList);
            }
            recorder.record(decision, "Stream element #" + countSeen + " = " + item + ": " + decision
                    + ".", state, List.of(countSeen), Map.of("i", countSeen));
        }
        long elapsed = System.nanoTime() - start;
        int len = Math.min(countSeen, k);
        List<Long> sample = box(reservoir, len);
        return new TracedResult("ReservoirSampling",
                Map.of("sample", sample, "k", k, "streamSize", countSeen),
                Map.of("steps", recorder.collect(), "truncated", recorder.isTruncated()),
                recorder.collect(), elapsed, "O(n) time, O(k) space", "O(k)");
    }

    private static List<Long> box(long[] values, int size) {
        List<Long> out = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            out.add(values[i]);
        }
        return out;
    }
}
