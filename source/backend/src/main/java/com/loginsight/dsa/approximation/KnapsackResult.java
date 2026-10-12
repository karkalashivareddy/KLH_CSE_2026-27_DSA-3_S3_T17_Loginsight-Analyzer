package com.loginsight.dsa.approximation;

/**
 * Immutable result of {@link KnapsackFPTAS}: the selected items, their total value and weight, the
 * scaled value used internally, and the running parameters ({@code eps}) plus the guarantee wording.
 */
public final class KnapsackResult {

    private final long[] weights;
    private final long[] values;
    private final long capacity;
    private final double eps;
    private final int[] selectedItems;
    private final int selectedCount;
    private final long totalValue;
    private final long totalWeight;
    private final long scaledValue;
    private final String guarantee;
    private final long executionTimeNanos;
    private final String timeComplexity;
    private final String spaceComplexity;

    KnapsackResult(long[] weights, long[] values, long capacity, double eps, int[] selectedItems,
                   long totalValue, long totalWeight, long scaledValue, String guarantee,
                   long executionTimeNanos, String timeComplexity, String spaceComplexity) {
        this.weights = weights;
        this.values = values;
        this.capacity = capacity;
        this.eps = eps;
        this.selectedItems = selectedItems;
        this.selectedCount = selectedItems.length;
        this.totalValue = totalValue;
        this.totalWeight = totalWeight;
        this.scaledValue = scaledValue;
        this.guarantee = guarantee;
        this.executionTimeNanos = executionTimeNanos;
        this.timeComplexity = timeComplexity;
        this.spaceComplexity = spaceComplexity;
    }

    /** Selected original item indices, in item order. */
    public int[] getSelectedItems() {
        return selectedItems;
    }

    public int getSelectedCount() {
        return selectedCount;
    }

    public long getTotalValue() {
        return totalValue;
    }

    public long getTotalWeight() {
        return totalWeight;
    }

    /** Sum of the scaled values of the selected items. */
    public long getScaledValue() {
        return scaledValue;
    }

    public double getEps() {
        return eps;
    }

    public long getCapacity() {
        return capacity;
    }

    /** Worst-case guarantee wording issued by the FPTAS for this run's {@code eps}. */
    public String getGuarantee() {
        return guarantee;
    }

    public long getExecutionTimeNanos() {
        return executionTimeNanos;
    }

    public String getTimeComplexity() {
        return timeComplexity;
    }

    public String getSpaceComplexity() {
        return spaceComplexity;
    }

    @Override
    public String toString() {
        return "KnapsackFPTAS(eps=" + eps + "): value " + totalValue + ", weight " + totalWeight
                + " (" + selectedCount + " items)";
    }
}