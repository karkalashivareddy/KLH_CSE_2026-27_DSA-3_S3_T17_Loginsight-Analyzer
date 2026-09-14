/**
 * Aho-Corasick multi-pattern matching, built by hand from the trie up.
 *
 * <p>Contains the automaton {@link com.loginsight.dsa.string.aho.AhoCorasick}, its node
 * representation {@link com.loginsight.dsa.string.aho.AhoNode} (sorted parallel transition arrays
 * with a hand-written binary search), and the occurrence record
 * {@link com.loginsight.dsa.string.aho.PatternMatch}. Failure links and dictionary-suffix links are
 * computed with a hand-rolled breadth-first queue — no standard deque used (docs/02 §8).</p>
 */
package com.loginsight.dsa.string.aho;