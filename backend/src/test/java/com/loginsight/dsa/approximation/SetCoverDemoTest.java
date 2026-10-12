package com.loginsight.dsa.approximation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link SetCoverDemo}: greedy set cover covers the universe whenever possible, reports the
 * harmonic bound, is deterministic, tolerates duplicate elements inside sets, and rejects
 * out-of-range elements. Random instances verify that the chosen sets truly cover the universe.
 */
class SetCoverDemoTest {

    private final SetCoverDemo demo = new SetCoverDemo();

    @Test
    void coversUniverseInClassicInstance() {
        // universe {0,1,2,3}; sets {0,1,2}, {1,3}, {2,3} -> greedy picks {0,1,2} then {2,3} or {1,3}
        int[][] sets = {{0, 1, 2}, {1, 3}, {2, 3}};
        SetCoverResult result = demo.greedyCover(4, sets);
        assertTrue(result.isAllCovered());
        assertEquals(4, result.getCoveredCount());
        assertEquals(2, result.getSelectedSetCount());
        assertSetsCover(result, sets);
    }

    @Test
    void emptyUniverseIsCoveredByNothing() {
        SetCoverResult result = demo.greedyCover(0, new int[0][]);
        assertTrue(result.isAllCovered());
        assertEquals(0, result.getSelectedSetCount());
        assertEquals(0.0, result.getHarmonicBound(), 1e-9);
    }

    @Test
    void nonEmptyUniverseUncoverableFamilyAllCoveredFalse() {
        int[][] sets = {{0, 1}, {1, 2}};
        SetCoverResult result = demo.greedyCover(5, sets);
        assertTrue(!result.isAllCovered());
        assertTrue(result.getCoveredCount() < 5);
        assertSetsCover(result, sets);
    }

    @Test
    void emptySetsNeverHelp() {
        int[][] sets = {{}, {0}, {1}};
        SetCoverResult result = demo.greedyCover(2, sets);
        assertTrue(result.isAllCovered());
        assertEquals(result.getSelectedSetCount(), result.getGreedySteps());
    }

    @Test
    void duplicateElementsInsideSetsAreIgnored() {
        int[][] sets = {{0, 0, 1, 1, 1}, {2, 3, 0}};
        SetCoverResult result = demo.greedyCover(4, sets);
        assertTrue(result.isAllCovered());
        assertEquals(2, result.getSelectedSetCount());
    }

    @Test
    void singleSetCoveringUniverse() {
        int[][] sets = {{0, 1, 2, 3, 4}};
        SetCoverResult result = demo.greedyCover(5, sets);
        assertTrue(result.isAllCovered());
        assertEquals(1, result.getSelectedSetCount());
        assertEquals(1, result.getGreedySteps());
    }

    @Test
    void harmonicBoundIsHOfUniverseSize() {
        SetCoverResult result = demo.greedyCover(5, new int[][]{{0, 1, 2, 3, 4}});
        double expected = 1.0 + 1.0 / 2 + 1.0 / 3 + 1.0 / 4 + 1.0 / 5;
        assertEquals(expected, result.getHarmonicBound(), 1e-12);
    }

    @Test
    void deterministicTieBreakBySetIndex() {
        int[][] sets = {{0, 1}, {1, 2}, {2, 3}};
        SetCoverResult first = demo.greedyCover(4, sets);
        SetCoverResult second = demo.greedyCover(4, sets);
        assertSameSelection(first, second);
    }

    @Test
    void randomInstancesGreedyCoversWhenCoverable() {
        for (int seed = 1; seed <= 200; seed++) {
            ApproxTestSupport support = new ApproxTestSupport(seed);
            int universe = 1 + support.nextInt(8);
            int setCount = 1 + support.nextInt(6);
            int[][] sets = new int[setCount][];
            for (int s = 0; s < setCount; s++) {
                int size = support.nextInt(universe + 1);
                int[] members = new int[size];
                for (int i = 0; i < size; i++) {
                    members[i] = support.nextInt(universe);
                }
                sets[s] = members;
            }
            SetCoverResult result = demo.greedyCover(universe, sets);
            assertSetsCover(result, sets);
            assertTrue(result.getSelectedSetCount() <= universe, "never exceeds universe size");
        }
    }

    @Test
    void rejectsOutOfRangeAndNull() {
        assertThrows(IllegalArgumentException.class, () -> demo.greedyCover(3, new int[][]{{0}, {5}}));
        assertThrows(IllegalArgumentException.class, () -> demo.greedyCover(-1, new int[0][]));
        assertThrows(IllegalArgumentException.class, () -> demo.greedyCover(3, null));
    }

    @Test
    void resultCarriesMetadata() {
        SetCoverResult result = demo.greedyCover(3, new int[][]{{0, 1}, {2}});
        assertNotNull(result.getNotes());
        assertTrue(result.getExecutionTimeNanos() >= 0);
        assertEquals(3, result.getUniverseSize());
    }

    private static void assertSetsCover(SetCoverResult result, int[][] sets) {
        boolean[] covered = new boolean[result.getUniverseSize()];
        for (int setIndex : result.getSelectedSetIndices()) {
            for (int element : sets[setIndex]) {
                covered[element] = true;
            }
        }
        int coveredBySelection = 0;
        for (int e = 0; e < result.getUniverseSize(); e++) {
            if (covered[e]) {
                coveredBySelection++;
            }
        }
        assertEquals(result.getCoveredCount(), coveredBySelection,
                "coveredCount must equal the union of the chosen sets");
        assertEquals(result.isAllCovered(), coveredBySelection == result.getUniverseSize());
    }

    private static void assertSameSelection(SetCoverResult first, SetCoverResult second) {
        assertEquals(first.getSelectedSetIndices().length, second.getSelectedSetIndices().length);
        for (int i = 0; i < first.getSelectedSetIndices().length; i++) {
            assertEquals(first.getSelectedSetIndices()[i], second.getSelectedSetIndices()[i]);
        }
    }
}