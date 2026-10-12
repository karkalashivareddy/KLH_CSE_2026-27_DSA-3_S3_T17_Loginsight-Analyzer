package com.loginsight.dsa.dp.editdistance;

/**
 * Result of an edit-distance computation that supports reconstruction: the metric, the full DP
 * matrix (for teaching/visualisation) and the ordered edit script that turns the source string into
 * the target string.
 *
 * <p>The operation tokens are the public constants {@link #MATCH}, {@link #SUBSTITUTE},
 * {@link #INSERT}, {@link #DELETE} and {@link #TRANSPOSE}. The script is returned in forward order
 * (left to right over the source). MATCH and SUBSTITUTE consume one source and one target character;
 * DELETE consumes one source character; INSERT consumes one target character; TRANSPOSE consumes two
 * source and two target characters. Summing the unit costs of the tokens (MATCH = 0, all others = 1)
 * must reproduce {@link #getDistance()}.</p>
 */
public final class EditDistanceResult {

    public static final String MATCH = "MATCH";
    public static final String SUBSTITUTE = "SUBSTITUTE";
    public static final String INSERT = "INSERT";
    public static final String DELETE = "DELETE";
    public static final String TRANSPOSE = "TRANSPOSE";

    private final String algorithm;
    private final long distance;
    private final long[][] matrix;
    private final String[] operations;

    public EditDistanceResult(String algorithm, long distance, long[][] matrix, String[] operations) {
        this.algorithm = algorithm;
        this.distance = distance;
        this.matrix = matrix;
        this.operations = operations == null ? new String[0] : operations;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public long getDistance() {
        return distance;
    }

    /** The full {@code (n+1) x (m+1)} DP table; row 0 / column 0 are the base cases. */
    public long[][] getMatrix() {
        return matrix;
    }

    /** Forward-ordered edit script (source → target), one token per step. */
    public String[] getOperations() {
        return operations;
    }

    public int getOperationCount() {
        return operations.length;
    }

    @Override
    public String toString() {
        return algorithm + " distance=" + distance + ", " + operations.length + " operations";
    }
}
