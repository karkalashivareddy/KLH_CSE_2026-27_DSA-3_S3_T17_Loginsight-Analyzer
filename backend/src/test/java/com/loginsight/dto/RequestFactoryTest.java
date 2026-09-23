package com.loginsight.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.loginsight.dto.request.DistanceRequest;
import com.loginsight.dto.request.MatrixChainRequest;
import com.loginsight.dto.request.Scope;
import com.loginsight.dto.request.SearchRequest;
import com.loginsight.exception.InvalidQueryException;

/**
 * RequestFactory conversions and guards (docs/REBUILD_BASELINE Phase-2): the same typed records as
 * the canonical endpoints, with laboratory bounds enforced before any engine runs.
 */
class RequestFactoryTest {

    @Test
    void searchDefaultsToExplicitScope() {
        SearchRequest request = RequestFactory.search(
                Map.of("pattern", "ORE", "text", "LOG: ERROR ORE"));
        assertEquals("ORE", request.pattern());
        assertEquals(Scope.EXPLICIT, request.scope());
        assertTrue(request.doubleHash() == null || !request.doubleHash());
    }

    @Test
    void searchRejectsMissingPattern() {
        assertThrows(InvalidQueryException.class,
                () -> RequestFactory.search(Map.of("text", "haystack")));
    }

    @Test
    void editBuildsDistanceRequest() {
        DistanceRequest request = RequestFactory.edit(Map.of("a", "kitten", "b", "sitting"),
                true);
        assertEquals("kitten", request.a());
        assertEquals("sitting", request.b());
        assertTrue(request.showMatrix());
    }

    @Test
    void matrixChainCapsDimensionsForTheO3Bound() {
        MatrixChainRequest request = RequestFactory.matrixChain(Map.of(
                "dims", List.of(10, 20, 30, 40)));
        assertEquals(4, request.dims().length);
        assertThrows(InvalidQueryException.class, () -> RequestFactory.matrixChain(
                Map.of("dims", new long[45])));
    }

    @Test
    void flowConvertsGraphEdges() {
        com.loginsight.dto.request.FlowRequest request = RequestFactory.flow(Map.of(
                "source", "S", "sink", "T", "nodes", List.of("S", "A", "T"),
                "edges", List.of(
                        Map.of("from", "S", "to", "A", "capacity", 10),
                        Map.of("from", "A", "to", "T", "capacity", 5))));
        assertEquals("S", request.source());
        assertEquals(2, request.edges().length);
        assertEquals(10L, request.edges()[0].capacity());
    }

    @Test
    void flowRejectsMissingSink() {
        assertThrows(InvalidQueryException.class, () -> RequestFactory.flow(Map.of(
                "source", "S", "nodes", List.of("S"), "edges", List.of())));
    }
}