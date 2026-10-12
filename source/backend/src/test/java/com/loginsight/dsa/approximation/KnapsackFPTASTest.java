package com.loginsight.dsa.approximation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for the 0/1 knapsack FPTAS {@link KnapsackFPTAS}. The returned value must lie between the
 * optimum and {@code (1 - eps) * OPT} (verified against an independent exact DP oracle), the returned
 * weight must respect the capacity, and every edge case — empty input, all-zero values, capacity too
 * small, single item, invalid parameters — must be handled and documented.
 */
class KnapsackFPTASTest {

    private final KnapsackFPTAS fptas = new KnapsackFPTAS();

    @Test
    void classicTextbookInstance() {
        long[] weights = {2, 3, 4, 5};
        long[] values = {3, 4, 5, 6};
        KnapsackResult result = fptas.approximate(weights, values, 5, 0.1);
        assertEquals(5, result.getTotalWeight());
        assertEquals(7, result.getTotalValue());
        assertTrue(result.getTotalWeight() <= 5);
    }

    @Test
    void singleItemBelowCapacitySelected() {
        long[] weights = {7};
        long[] values = {9};
        KnapsackResult result = fptas.approximate(weights, values, 7, 0.5);
        assertEquals(1, result.getSelectedCount());
        assertEquals(9, result.getTotalValue());
        assertEquals(7, result.getTotalWeight());
    }

    @Test
    void capacityTooSmallYieldsEmptySelection() {
        long[] weights = {5, 6, 7};
        long[] values = {10, 11, 12};
        KnapsackResult result = fptas.approximate(weights, values, 4, 0.5);
        assertEquals(0, result.getSelectedCount());
        assertEquals(0, result.getTotalValue());
        assertEquals(0, result.getTotalWeight());
    }

    @Test
    void allItemsFitWhenCapacityIsLargeEnough() {
        long[] weights = {2, 3, 4};
        long[] values = {5, 6, 7};
        KnapsackResult result = fptas.approximate(weights, values, 100, 0.5);
        assertEquals(3, result.getSelectedCount());
        assertEquals(18, result.getTotalValue());
        assertEquals(9, result.getTotalWeight());
    }

    @Test
    void allZeroValuesReturnEmptySelection() {
        long[] weights = {2, 3, 4};
        long[] values = {0, 0, 0};
        KnapsackResult result = fptas.approximate(weights, values, 5, 0.5);
        assertEquals(0, result.getSelectedCount());
        assertEquals(0, result.getTotalValue());
    }

    @Test
    void emptyInstanceReturnedWithoutScaling() {
        KnapsackResult result = fptas.approximate(new long[0], new long[0], 10, 0.5);
        assertEquals(0, result.getSelectedCount());
        assertEquals(0, result.getTotalValue());
        assertEquals(0.5, result.getEps());
    }

    @Test
    void guaranteeHoldsAcrossRandomInstances() {
        double[] epsilons = {0.1, 0.2, 0.5};
        for (int seed = 1; seed <= 300; seed++) {
            ApproxTestSupport support = new ApproxTestSupport(seed);
            long[][] instance = support.randomKnapsack(1 + support.nextInt(8), 12, 40);
            long[] weights = instance[0];
            long[] values = instance[1];
            long capacity = instance[2][0];
            long optimum = ApproxTestSupport.exactKnapsack(weights, values, capacity);
            for (double eps : epsilons) {
                KnapsackResult result = fptas.approximate(weights, values, capacity, eps);
                assertTrue(result.getTotalWeight() <= capacity,
                        "feasibility, seed " + seed + " eps=" + eps);
assertTrue(result.getTotalValue() <= optimum,
                        "can never exceed the optimum, seed " + seed + " eps=" + eps
                                + " A=" + result.getTotalValue() + " OPT=" + optimum
                                + " n=" + weights.length + " cap=" + capacity);
                assertTrue(result.getTotalValue() >= (1 - eps) * optimum - 1e-9,
                        "guarantee A >= (1-eps)OPT, seed " + seed + " eps=" + eps
                                + " A=" + result.getTotalValue() + " OPT=" + optimum);
                assertTrue(result.getSelectedCount() <= weights.length);
            }
        }
    }

    @Test
    void smallerEpsNeverHurtsTheValue() {
        long[] weights = {3, 4, 5, 6, 7, 8};
        long[] values = {4, 5, 6, 7, 8, 9};
        long capacity = 20;
        KnapsackResult coarse = fptas.approximate(weights, values, capacity, 0.5);
        KnapsackResult fine = fptas.approximate(weights, values, capacity, 0.1);
        assertTrue(fine.getTotalValue() >= coarse.getTotalValue(),
                "shrinking eps must not degrade the answer");
    }

    @Test
    void deterministicAcrossRuns() {
        long[] weights = {2, 3, 4, 5, 6};
        long[] values = {3, 4, 5, 6, 7};
        KnapsackResult first = fptas.approximate(weights, values, 10, 0.2);
        KnapsackResult second = fptas.approximate(weights, values, 10, 0.2);
        assertEquals(first.getTotalValue(), second.getTotalValue());
        assertEquals(first.getSelectedItems().length, second.getSelectedItems().length);
        for (int i = 0; i < first.getSelectedItems().length; i++) {
            assertEquals(first.getSelectedItems()[i], second.getSelectedItems()[i]);
        }
    }

    @Test
    void rejectsInvalidInput() {
        long[] weights = {1, 2};
        long[] values = {3, 4};
        assertThrows(IllegalArgumentException.class, () -> fptas.approximate(null, values, 5, 0.5));
        assertThrows(IllegalArgumentException.class, () -> fptas.approximate(weights, null, 5, 0.5));
        assertThrows(IllegalArgumentException.class, () -> fptas.approximate(new long[]{1}, new long[]{1, 2}, 5, 0.5));
        assertThrows(IllegalArgumentException.class, () -> fptas.approximate(weights, values, -1, 0.5));
        assertThrows(IllegalArgumentException.class, () -> fptas.approximate(weights, values, 5, 0.0));
        assertThrows(IllegalArgumentException.class, () -> fptas.approximate(weights, values, 5, 1.0));
        assertThrows(IllegalArgumentException.class, () -> fptas.approximate(weights, values, 5, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> fptas.approximate(new long[]{0, 1}, values, 5, 0.5));
        assertThrows(IllegalArgumentException.class, () -> fptas.approximate(weights, new long[]{1, -1}, 5, 0.5));
    }
}