/**
 * Suffix structures for substring queries over log lines.
 *
 * <p>{@link com.loginsight.dsa.string.suffix.SuffixArray} builds the lexicographically ordered
 * suffix array by prefix doubling with a hand-written counting (radix) sort, and answers substring
 * queries by binary search. {@link com.loginsight.dsa.string.suffix.KasaiLCP} computes the longest
 * common prefix array over it in linear time using the standard Kasai invariant.</p>
 */
package com.loginsight.dsa.string.suffix;