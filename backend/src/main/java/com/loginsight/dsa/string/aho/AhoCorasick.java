package com.loginsight.dsa.string.aho;

import java.util.Arrays;

import com.loginsight.dsa.string.StringSearchResult;

/**
 * Algorithm: Aho-Corasick multi-pattern exact matching.
 * <p>
 * Purpose: find every occurrence of every pattern from a set in one pass over the text, using one
 * trie augmented with failure links and dictionary-suffix links.
 * <p>
 * Input: a non-empty array of non-empty patterns given at construction; a text at match time.
 * <p>
 * Output: a {@link PatternMatch} for every occurrence (pattern id, start position); the DSA result
 * additionally aggregates every start position in scan order.
 * <p>
 * Preprocessing (O(growth) of the trie): insert patterns into a sorted-transition trie, then
 * breadth-first compute:
 * <ul>
 *   <li>{@code fail[v]}: the deepest proper suffix of the string that reaches v which is also a
 *       trie prefix(such a state may exist among previously inserted patterns).</li>
 *   <li>{@code dictSuffix[v]}: the nearest node reachable from {@code fail[v]} through the fail
 *       chain that terminates some pattern — used to emit all pattern-suffix matches without
 *       re-walking the fail chain per character.</li>
 * </ul>
 * <p>
 * Time Complexity: the search is O(n + number of reported matches). The per-character transition
 * includes a binary search over the sorted transition list, giving a documented
 * O((n + Σ|P|) · log σ + matches), σ = alphabet viewed through UTF-16 code units. Space
 * Complexity: O(Σ|P|) for trie plus failure metadata.
 * <p>
 * Duplicate handling: if the same pattern text appears twice in the input array, the first id wins
 * for recording purposes (the redundant second pattern is skipped, so no double-reporting).
 * <p>
 * Empty-contract: null or empty pattern array rejected; any null or empty pattern rejected. Handling
 * only after preprocessing, so an {@code IllegalArgumentException} may surface either at
 * construction or at first match.
 */
public final class AhoCorasick {

    private static final String ALGORITHM = "Aho-Corasick";

    private final String[] patterns;
    private AhoNode[] nodes;
    private int nodeCount;

    public AhoCorasick(String[] patterns) {
        if (patterns == null || patterns.length == 0) {
            throw new IllegalArgumentException("patterns must not be null or empty");
        }
        this.patterns = patterns.clone();
        for (String pattern : this.patterns) {
            if (pattern == null || pattern.isEmpty()) {
                throw new IllegalArgumentException("each pattern must be non-empty and non-null");
            }
        }
        this.nodes = new AhoNode[16];
        build();
    }

    /** Build the trie, then compute failure + dict-suffix links via a hand-rolled BFS queue. */
    private void build() {
        int root = newNode(0);
        for (int id = 0; id < patterns.length; id++) {
            char[] pattern = patterns[id].toCharArray();
            int state = 0;
            for (char c : pattern) {
                int child = nodes[state].findChild(c);
                if (child == -1) {
                    child = newNode(nodes[state].getDepth() + 1);
                    nodes[state].addChild(c, child);
                }
                state = child;
            }
            if (nodes[state].getOutput() == -1) {
                nodes[state].setOutput(id);
            }
        }

        int[] queue = new int[nodeCount];
        int head = 0;
        int tail = 0;
        for (int i = 0; i < nodes[root].childCount(); i++) {
            int child = nodes[root].childAt(i);
            nodes[child].setFail(0);
            nodes[child].setDictSuffix(-1);
            queue[tail++] = child;
        }
        while (head < tail) {
            int v = queue[head++];
            int failV = nodes[v].getFail();
            for (int i = 0; i < nodes[v].childCount(); i++) {
                char c = nodes[v].childCharAt(i);
                int u = nodes[v].childAt(i);
                int f = failV;
                while (f != 0 && nodes[f].findChild(c) == -1) {
                    f = nodes[f].getFail();
                }
                int g = nodes[f].findChild(c);
                int fail = g == -1 ? 0 : g;
                nodes[u].setFail(fail);
                nodes[u].setDictSuffix(nodes[fail].getOutput() != -1 ? fail : nodes[fail].getDictSuffix());
                queue[tail++] = u;
            }
        }
    }

    private int newNode(int depth) {
        if (nodeCount == nodes.length) {
            nodes = Arrays.copyOf(nodes, nodes.length * 2);
        }
        nodes[nodeCount] = new AhoNode(depth);
        int index = nodeCount;
        nodeCount++;
        return index;
    }

    /**
     * Search the whole text once and return every reported occurrence in scan order (matches whose
     * patterns end at the same position are ordered by depth, deepest first).
     */
    public PatternMatch[] search(String text) {
        if (text == null) {
            throw new IllegalArgumentException("text must not be null");
        }
        char[] chars = text.toCharArray();
        int[] ids = new int[16];
        int[] starts = new int[16];
        int matchCount = 0;

        int state = 0;
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            while (state != 0 && nodes[state].findChild(c) == -1) {
                state = nodes[state].getFail();
            }
            int g = nodes[state].findChild(c);
            state = g == -1 ? 0 : g;
            // Report the node itself, then every output-bearing suffix via dict-suffix links
            // (the chain terminates at -1 when no output-bearing suffix exists).
            for (int v = state; v != 0 && v != -1; v = nodes[v].getDictSuffix()) {
                int id = nodes[v].getOutput();
                if (id != -1) {
                    ids = ensureCapacity(ids, matchCount);
                    starts = ensureCapacity(starts, matchCount);
                    ids[matchCount] = id;
                    starts[matchCount] = i - patterns[id].length() + 1;
                    matchCount++;
                }
            }
        }

        PatternMatch[] matches = new PatternMatch[matchCount];
        for (int i = 0; i < matchCount; i++) {
            matches[i] = new PatternMatch(ids[i], patterns[ids[i]], starts[i]);
        }
        return matches;
    }

    /** All matches surfaced as a {@link StringSearchResult}, matching the single-matcher shape. */
    public StringSearchResult match(String text) {
        long start = System.nanoTime();
        PatternMatch[] matches = search(text);
        long elapsed = System.nanoTime() - start;

        int[] positions = new int[matches.length];
        StringBuilder header = new StringBuilder();
        for (int i = 0; i < matches.length; i++) {
            positions[i] = matches[i].getStart();
        }
        for (int i = 0; i < patterns.length; i++) {
            if (i > 0) {
                header.append(", ");
            }
            header.append(patterns[i]);
        }

        return new StringSearchResult(ALGORITHM, header.toString(), text == null ? 0 : text.length(),
                positions, elapsed, "O((n + Σ|P|)·log σ + matches)", "O(Σ|P|)", matches);
    }

    private static int[] ensureCapacity(int[] array, int required) {
        if (required < array.length) {
            return array;
        }
        int[] grown = new int[array.length * 2];
        System.arraycopy(array, 0, grown, 0, required);
        return grown;
    }

    public int patternCount() {
        return patterns.length;
    }

    public int nodeCount() {
        return nodeCount;
    }

    /** Failure-link inspection, primarily for tests that verify the reference trie. */
    public int failureOf(int node) {
        return nodes[node].getFail();
    }

    /** Output inspection for a node: the pattern id ending there, or -1. */
    public int outputAt(int node) {
        return nodes[node].getOutput();
    }
}