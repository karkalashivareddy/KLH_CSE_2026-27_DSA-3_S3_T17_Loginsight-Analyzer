package com.loginsight.dsa.randomized;

/**
 * Educational hash-table demonstration built on a {@link UniversalHashFamily}.
 *
 * <p>Inserts a static key set into a simple open-addressed or bucket-counting structure
 * using the supplied family and records collision statistics.  This is <em>not</em> a
 * disguised {@code HashMap}; its purpose is to make the collision behaviour of universal
 * hashing visible to the learner.</p>
 *
 * <h2>Usage</h2>
 * <pre>
 *   long[] keys = {10, 25, 40, 55, 70};
 *   UniversalHashFamily family = UniversalHashFamily.random(DEFAULT_PRIME, 7, rng);
 *   RandomizedHash demo = new RandomizedHash(keys, family);
 *   HashStatistics stats = demo.getStatistics();
 *   // stats.collisionPairs() counts the number of keys that collided with an earlier key.
 * </pre>
 *
 * <p>All state is computed at construction time; subsequent calls to {@link #hash(long)} are
 * pure lookups against the same family.</p>
 */
public final class RandomizedHash {

    private final UniversalHashFamily family;
    private final long[] keys;
    private final HashStatistics statistics;

    /**
     * @param keys   the key set (a defensive copy is stored)
     * @param family the universal hash family to use
     */
    public RandomizedHash(long[] keys, UniversalHashFamily family) {
        this.family = family;
        this.keys = keys.clone();
        this.statistics = analyze(keys, family);
    }

    /** Hashes {@code key} using the underlying family. */
    public int hash(long key) {
        return family.hash(key);
    }

    /** Collision and distribution statistics for the key set. */
    public HashStatistics getStatistics() {
        return statistics;
    }

    /** The underlying universal hash family. */
    public UniversalHashFamily getFamily() {
        return family;
    }

    private static HashStatistics analyze(long[] keys, UniversalHashFamily family) {
        int m = family.getTableSize();
        int[] bucketCounts = new int[m];
        int collisions = 0;
        for (long key : keys) {
            int h = family.hash(key);
            if (bucketCounts[h] > 0) {
                collisions++;
            }
            bucketCounts[h]++;
        }
        int maxChain = 0;
        for (int c : bucketCounts) {
            if (c > maxChain) {
                maxChain = c;
            }
        }
        return new HashStatistics(m, keys.length, collisions, maxChain);
    }

    /** Immutable collision and distribution statistics. */
    public static final class HashStatistics {

        private final int tableSize;
        private final int keyCount;
        private final int collisionPairs;
        private final int maxBucketSize;
        private final double loadFactor;

        HashStatistics(int tableSize, int keyCount, int collisionPairs, int maxBucketSize) {
            this.tableSize = tableSize;
            this.keyCount = keyCount;
            this.collisionPairs = collisionPairs;
            this.maxBucketSize = maxBucketSize;
            this.loadFactor = tableSize == 0 ? 0.0 : (double) keyCount / tableSize;
        }

        /** Number of hash-table slots. */
        public int tableSize() {
            return tableSize;
        }

        /** Number of keys inserted. */
        public int keyCount() {
            return keyCount;
        }

        /**
         * Number of keys that collided with a previously inserted key (a measure of how
         * many keys shared a bucket with at least one earlier key).
         */
        public int collisionPairs() {
            return collisionPairs;
        }

        /** Number of keys in the fullest bucket. */
        public int maxBucketSize() {
            return maxBucketSize;
        }

        /** Key count divided by table size. */
        public double loadFactor() {
            return loadFactor;
        }

        @Override
        public String toString() {
            return "HashStatistics{m=" + tableSize + ", n=" + keyCount
                    + ", collisions=" + collisionPairs + ", maxChain=" + maxBucketSize
                    + ", loadFactor=" + loadFactor + '}';
        }
    }
}
