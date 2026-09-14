/**
 * String algorithms module (DSA-3 Module 2/3): all exact string matching and suffix-structure
 * algorithms used by the log-insight query layer.
 *
 * <p>Implemented in Phase 3 (docs/03, docs/04):</p>
 * <ul>
 *   <li>{@link com.loginsight.dsa.string.NaiveMatcher} — baseline O(n·m) search; the reference every
 *       other matcher is cross-checked against.</li>
 *   <li>{@link com.loginsight.dsa.string.KMPMatcher} — KMP with a hand-built LPS failure function.</li>
 *   <li>{@link com.loginsight.dsa.string.ZAlgorithm} — Z-function search with a [l, r] window.</li>
 *   <li>{@link com.loginsight.dsa.string.RabinKarpMatcher} — polynomial rolling hash with mandatory
 *       character verification and an optional second independent hash.</li>
 *   <li>{@link com.loginsight.dsa.string.aho.AhoCorasick} — multi-pattern automaton (trie + fail links +
 *       dictionary-suffix links).</li>
 *   <li>{@link com.loginsight.dsa.string.suffix.SuffixArray} — suffix array by prefix doubling with a
 *       hand-written counting sort + binary-search substring search.</li>
 *   <li>{@link com.loginsight.dsa.string.suffix.KasaiLCP} — longest-common-prefix array over the suffix
 *       array in linear time.</li>
 * </ul>
 *
 * <p>Every matcher returns the uniform {@link com.loginsight.dsa.string.StringSearchResult} and shares
 * one input contract: null or empty patterns are rejected, a pattern longer than the text yields an
 * empty result. Algorithms here are pure Java — no Spring, no {@code java.util} collections/sorting,
 * no {@code String.indexOf}/regex delegating the core logic (docs/02 §8).</p>
 *
 * <p>Character model: all implementations operate on UTF-16 {@code char} units. Positions reported
 * in results are code-unit offsets, meaning astral-plane characters are counted as two positions.
 * Tests deliberately include multilingual (Indian-language) input so the contract stays honest.</p>
 */
package com.loginsight.dsa.string;