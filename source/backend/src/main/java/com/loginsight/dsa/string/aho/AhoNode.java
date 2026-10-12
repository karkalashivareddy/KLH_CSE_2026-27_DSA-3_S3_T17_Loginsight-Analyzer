package com.loginsight.dsa.string.aho;

/**
 * A trie node with a compact, sorted transition representation.
 *
 * <p>Transitions are stored as two parallel growable arrays: {@code keys} (the sorted child
 * characters) and {@code next} (the child node index for each key). Lookup is a hand-written
 * binary search, so no {@code java.util.Map} is used to hide the trie structure (docs/02 §8). The
 * character model is UTF-16 code units, which makes the representation independent of the input
 * language.</p>
 */
public final class AhoNode {

    private char[] keys = new char[0];
    private int[] next = new int[0];

    /** Failure link: state to continue from after a mismatch at this node. */
    private int fail;

    /** Pattern index ending exactly at this node, or -1. */
    private int output = -1;

    /** Dictionary-suffix link: nearest node reachable via fail chain that has an output. */
    private int dictSuffix = -1;

    private int depth;

    AhoNode(int depth) {
        this.depth = depth;
    }

    /** Node index of the child labelled {@code c}, or -1 when no such transition exists. */
    public int findChild(char c) {
        int lo = 0;
        int hi = keys.length - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            int cmp = keys[mid] - c;
            if (cmp == 0) {
                return next[mid];
            }
            if (cmp < 0) {
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return -1;
    }

    /** Insert transition {@code c -> node}, preserving sorted order. */
    void addChild(char c, int node) {
        int pos = insertionPoint(c);
        char[] newKeys = new char[keys.length + 1];
        int[] newNext = new int[next.length + 1];
        System.arraycopy(keys, 0, newKeys, 0, pos);
        System.arraycopy(next, 0, newNext, 0, pos);
        newKeys[pos] = c;
        newNext[pos] = node;
        System.arraycopy(keys, pos, newKeys, pos + 1, keys.length - pos);
        System.arraycopy(next, pos, newNext, pos + 1, next.length - pos);
        keys = newKeys;
        next = newNext;
    }

    private int insertionPoint(char c) {
        int lo = 0;
        int hi = keys.length;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (keys[mid] < c) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        return lo;
    }

    public int childCount() {
        return keys.length;
    }

    public char childCharAt(int index) {
        return keys[index];
    }

    public int childAt(int index) {
        return next[index];
    }

    public int getFail() {
        return fail;
    }

    void setFail(int fail) {
        this.fail = fail;
    }

    public int getOutput() {
        return output;
    }

    void setOutput(int output) {
        this.output = output;
    }

    public int getDictSuffix() {
        return dictSuffix;
    }

    void setDictSuffix(int dictSuffix) {
        this.dictSuffix = dictSuffix;
    }

    public int getDepth() {
        return depth;
    }
}