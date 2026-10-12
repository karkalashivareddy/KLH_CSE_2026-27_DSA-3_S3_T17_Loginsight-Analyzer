package com.loginsight.dsa.randomized;

/**
 * FKS-style perfect hash table (Fredman, Komlós, Szemerédi, 1984).
 *
 * <p>Given a static set of <b>distinct</b> {@code long} keys, builds a two-level hash
 * table that guarantees <b>zero collisions</b> on every lookup.  Membership queries
 * ({@link #contains(long)}) execute in worst-case O(1) time.</p>
 *
 * <h2>Construction outline</h2>
 * <ol>
 *   <li>Choose {@code m = n} first-level buckets using a universal hash
 *       {@code h(x) = ((firstA·x + firstB) mod p) mod m}.</li>
 *   <li>For each bucket {@code j} with {@code b_j} keys, allocate a secondary table of
 *       size {@code m_j = b_j²}.  Pick a fresh random
 *       {@code h_j(x) = ((a_j·x + b_j) mod p) mod m_j} and rebuild until no two keys
 *       in the bucket collide.  A retry cap of 128 prevents infinite loops.</li>
 *   <li>All keys and empty slots are stored in a single flat {@code long[]} array.
 *       Empty slots contain a {@link #sentinel} value not present in the key set.</li>
 * </ol>
 *
 * <h2>Complexity</h2>
 * <ul>
 *   <li><b>Time</b>: O(1) worst-case per {@link #contains} (two hash computations + one
 *       array access).</li>
 *   <li><b>Space</b>: expected O(n) ({@code E[Σ m_j] = E[Σ b_j²] ≈ 2n} when {@code m = n}),
 *       worst-case O(n²) when all keys fall into one bucket.</li>
 * </ul>
 *
 * <h2>Mutability</h2>
 * <p>This is a <b>static, read-only</b> structure.  {@link #insert} and {@link #remove}
 * throw {@link UnsupportedOperationException}.</p>
 */
public final class PerfectHash {

    private static final long PRIME = UniversalHashFamily.DEFAULT_PRIME;
    private static final int MAX_RETRY = 128;
    private static final int MAX_SECONDARY_SIZE = 46340; // floor(sqrt(Integer.MAX_VALUE))

    private final int n;
    private final long sentinel;
    private final long firstA;
    private final long firstB;

    private final int[] bucketStart;
    private final int[] bucketSizes;
    private final long[] secondaryA;
    private final long[] secondaryB;
    private final long[] slotKeys;
    private final int totalSlots;

    /**
     * Builds a perfect hash table from the given distinct keys.
     *
     * @param keys distinct long keys (a defensive copy is stored)
     * @param rng  random source for hash-parameter generation
     * @throws IllegalStateException if construction fails within the retry limit
     * @throws IllegalArgumentException if {@code keys} is null or contains duplicates
     */
    public PerfectHash(long[] keys, RandomSource rng) {
        if (keys == null) {
            throw new IllegalArgumentException("keys must not be null");
        }
        this.n = keys.length;
        rejectDuplicates(keys);
        this.sentinel = findSentinel(keys);

        if (n == 0) {
            this.firstA = 0;
            this.firstB = 0;
            this.bucketStart = new int[0];
            this.bucketSizes = new int[0];
            this.secondaryA = new long[0];
            this.secondaryB = new long[0];
            this.slotKeys = new long[0];
            this.totalSlots = 0;
            return;
        }

        // First-level parameters
        this.firstA = 1 + rng.nextLong(PRIME - 1);
        this.firstB = rng.nextLong(PRIME);

        // Bucket keys
        long[][] buckets = new long[n][];
        int[] bucketCounts = new int[n];
        for (long key : keys) {
            int b = firstHash(key);
            bucketCounts[b]++;
        }
        for (int i = 0; i < n; i++) {
            buckets[i] = new long[bucketCounts[i]];
            bucketCounts[i] = 0;
        }
        for (long key : keys) {
            int b = firstHash(key);
            buckets[b][bucketCounts[b]++] = key;
        }

        // Secondary tables
        this.bucketStart = new int[n];
        this.bucketSizes = new int[n];
        this.secondaryA = new long[n];
        this.secondaryB = new long[n];

        int cumulativeSize = 0;
        for (int j = 0; j < n; j++) {
            int bj = buckets[j].length;
            if (bj == 0) {
                bucketStart[j] = cumulativeSize;
                bucketSizes[j] = 0;
                continue;
            }
            if (bj > MAX_SECONDARY_SIZE) {
                throw new IllegalStateException(
                        "Bucket " + j + " has " + bj + " keys, exceeding secondary table limit");
            }
            int mj = bj * bj;
            bucketSizes[j] = mj;
            bucketStart[j] = cumulativeSize;

            // Try random secondary hash parameters until collision-free
            boolean placed = false;
            for (int attempt = 0; attempt < MAX_RETRY; attempt++) {
                long a = 1 + rng.nextLong(PRIME - 1);
                long b = rng.nextLong(PRIME);
                if (isCollisionFree(buckets[j], mj, a, b)) {
                    secondaryA[j] = a;
                    secondaryB[j] = b;
                    placed = true;
                    break;
                }
            }
            if (!placed) {
                throw new IllegalStateException(
                        "Failed to find collision-free secondary hash for bucket " + j
                                + " within " + MAX_RETRY + " attempts");
            }
            cumulativeSize += mj;
        }
        this.totalSlots = cumulativeSize;
        this.slotKeys = new long[totalSlots];

        // Fill sentinel into all slots first
        for (int i = 0; i < totalSlots; i++) {
            slotKeys[i] = sentinel;
        }

        // Place keys
        for (int j = 0; j < n; j++) {
            for (long key : buckets[j]) {
                int slot = secondaryHash(key, j);
                slotKeys[bucketStart[j] + slot] = key;
            }
        }
    }

    /**
     * Returns {@code true} when {@code key} is in the perfect hash table.
     *
     * <p>Worst-case O(1): two hash computations + one comparison.  The guard
     * {@code key != sentinel} rejects the sentinel query value itself: an empty slot stores
     * the sentinel, so without it a non-key whose hash lands on an empty slot would be a
     * false positive.  Because the sentinel is chosen outside the key set by construction,
     * the guard never rejects a genuine key.</p>
     */
    public boolean contains(long key) {
        if (n == 0) {
            return false;
        }
        int b = firstHash(key);
        if (bucketSizes[b] == 0) {
            return false;
        }
        int slot = secondaryHash(key, b);
        return slotKeys[bucketStart[b] + slot] == key && key != sentinel;
    }

    /** Number of distinct keys. */
    public int size() {
        return n;
    }

    /** Sentinel value not present in the key set. */
    public long getSentinel() {
        return sentinel;
    }

    /** Total secondary-table slots (sum of all m_j). */
    public int getTotalSecondarySlots() {
        return totalSlots;
    }

    /** Maximum secondary table size (max m_j). */
    public int getMaxSecondarySize() {
        int max = 0;
        for (int s : bucketSizes) {
            if (s > max) {
                max = s;
            }
        }
        return max;
    }

    /** Number of first-level buckets (equals {@code n}). */
    public int getFirstLevelSize() {
        return n;
    }

    public long getFirstA() {
        return firstA;
    }

    public long getFirstB() {
        return firstB;
    }

    public long getFirstPrime() {
        return PRIME;
    }

    /** Always throws — this is a static, read-only structure. */
    public void insert(long key) {
        throw new UnsupportedOperationException("PerfectHash is static and read-only");
    }

    /** Always throws — this is a static, read-only structure. */
    public void remove(long key) {
        throw new UnsupportedOperationException("PerfectHash is static and read-only");
    }

    private int firstHash(long key) {
        long x = key % PRIME;
        if (x < 0) {
            x += PRIME;
        }
        long ax = ModularArithmetic.multiplyMod(firstA, x, PRIME);
        long result = ModularArithmetic.addMod(ax, firstB, PRIME);
        return (int) (result % n);
    }

    private int secondaryHash(long key, int bucket) {
        long x = key % PRIME;
        if (x < 0) {
            x += PRIME;
        }
        long ax = ModularArithmetic.multiplyMod(secondaryA[bucket], x, PRIME);
        long result = ModularArithmetic.addMod(ax, secondaryB[bucket], PRIME);
        return (int) (result % bucketSizes[bucket]);
    }

    private boolean isCollisionFree(long[] bucketKeys, int mj, long a, long b) {
        boolean[] occupied = new boolean[mj];
        for (long key : bucketKeys) {
            long x = key % PRIME;
            if (x < 0) {
                x += PRIME;
            }
            long ax = ModularArithmetic.multiplyMod(a, x, PRIME);
            long result = ModularArithmetic.addMod(ax, b, PRIME);
            int slot = (int) (result % mj);
            if (occupied[slot]) {
                return false;
            }
            occupied[slot] = true;
        }
        return true;
    }

    /**
     * Throws {@link IllegalArgumentException} if {@code keys} contains a duplicate.
     *
     * <p>Duplicates could never be stored (two identical keys always hash to the same
     * secondary slot, so the rebuild loop would burn all 128 attempts and throw a confusing
     * {@link IllegalStateException}).  Rejecting them explicitly keeps the documented
     * contract: the input is a <em>set</em> of distinct keys.  O(n²); intended for the
     * small static key sets this educational structure is designed for.</p>
     */
    private static void rejectDuplicates(long[] keys) {
        for (int i = 0; i < keys.length; i++) {
            for (int j = i + 1; j < keys.length; j++) {
                if (keys[i] == keys[j]) {
                    throw new IllegalArgumentException(
                            "keys must be distinct; duplicate value: " + keys[i]);
                }
            }
        }
    }

    /**
     * Finds a {@code long} value not present among the keys.
     * Tries {@code 0, 1, −1, 2, −2, …} and falls back to {@code Long.MIN_VALUE}.
     */
    private static long findSentinel(long[] keys) {
        long[] candidates = {0L, 1L, -1L, 2L, -2L, 3L, -3L, 4L, -4L, 5L, -5L};
        for (long candidate : candidates) {
            boolean found = false;
            for (long k : keys) {
                if (k == candidate) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return candidate;
            }
        }
        return Long.MIN_VALUE;
    }
}
