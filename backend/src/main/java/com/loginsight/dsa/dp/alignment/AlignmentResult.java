package com.loginsight.dsa.dp.alignment;

/**
 * Result of a sequence alignment (global Needleman-Wunsch or local Smith-Waterman).
 *
 * <p>Both alignments are represented as parallel token arrays; a gap is the token {@link #GAP}
 * ("-"). The two arrays always have the same length. For global alignment the whole of sequence A and
 * sequence B is represented (trailing/leading gaps included); for local alignment only the aligned
 * core region is represented and {@code startA..endA} / {@code startB..endB} give the half-open
 * interval inside the original sequences that the core covers.</p>
 *
 * <p>The matrix is the full {@code (n+1) x (m+1)} score table, exposed for DP-matrix visualisation.</p>
 */
public final class AlignmentResult {

    /** Gap symbol used in the aligned token arrays. */
    public static final String GAP = "-";

    private final String algorithm;
    private final long score;
    private final String[] alignedA;
    private final String[] alignedB;
    private final long[][] matrix;
    private final int startA;
    private final int startB;
    private final int endA;
    private final int endB;

    public AlignmentResult(String algorithm, long score, String[] alignedA, String[] alignedB,
                           long[][] matrix, int startA, int startB, int endA, int endB) {
        this.algorithm = algorithm;
        this.score = score;
        this.alignedA = alignedA;
        this.alignedB = alignedB;
        this.matrix = matrix;
        this.startA = startA;
        this.startB = startB;
        this.endA = endA;
        this.endB = endB;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public long getScore() {
        return score;
    }

    public String[] getAlignedA() {
        return alignedA;
    }

    public String[] getAlignedB() {
        return alignedB;
    }

    public long[][] getMatrix() {
        return matrix;
    }

    /** Start index in sequence A of the aligned region (always 0 for global alignment). */
    public int getStartA() {
        return startA;
    }

    public int getStartB() {
        return startB;
    }

    /** Half-open end index in sequence A of the aligned region (always n for global alignment). */
    public int getEndA() {
        return endA;
    }

    public int getEndB() {
        return endB;
    }

    public int getAlignmentLength() {
        return alignedA.length;
    }

    public int getGapCount() {
        int gaps = 0;
        for (int i = 0; i < alignedA.length; i++) {
            if (GAP.equals(alignedA[i]) || GAP.equals(alignedB[i])) {
                gaps++;
            }
        }
        return gaps;
    }

    @Override
    public String toString() {
        return algorithm + " score=" + score + " (length " + alignedA.length + ")";
    }
}
