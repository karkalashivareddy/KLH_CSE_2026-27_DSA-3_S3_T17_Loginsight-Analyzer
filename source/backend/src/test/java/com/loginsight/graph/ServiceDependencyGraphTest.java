package com.loginsight.graph;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.loginsight.model.LogEvent;
import com.loginsight.model.LogLevel;

class ServiceDependencyGraphTest {

    @Test
    void addNodeCreatesEntry() {
        ServiceDependencyGraph graph = new ServiceDependencyGraph();
        graph.addNode("AUTH");
        assertEquals(1, graph.nodeCount());
        assertEquals(0, graph.edgeCount());
        assertTrue(graph.nodes().contains("AUTH"));
    }

    @Test
    void addDependencyCreatesNodesAndEdge() {
        ServiceDependencyGraph graph = new ServiceDependencyGraph();
        graph.addDependency("AUTH", "USER");
        assertEquals(2, graph.nodeCount());
        assertEquals(1, graph.edgeCount());
        assertTrue(graph.successors("AUTH").contains("USER"));
        assertTrue(graph.predecessors("USER").contains("AUTH"));
    }

    @Test
    void selfLoopIsIgnored() {
        ServiceDependencyGraph graph = new ServiceDependencyGraph();
        graph.addDependency("AUTH", "AUTH");
        assertEquals(1, graph.nodeCount());
        assertEquals(0, graph.edgeCount());
    }

    @Test
    void duplicateEdgesAreDeduplicated() {
        ServiceDependencyGraph graph = new ServiceDependencyGraph();
        graph.addDependency("AUTH", "USER");
        graph.addDependency("AUTH", "USER");
        assertEquals(1, graph.edgeCount());
    }

    @Test
    void reachableFromReturnsBfsOrder() {
        ServiceDependencyGraph graph = new ServiceDependencyGraph();
        graph.addDependency("A", "B");
        graph.addDependency("A", "C");
        graph.addDependency("B", "D");
        graph.addDependency("C", "D");
        graph.addDependency("D", "E");

        List<String> reachable = graph.reachableFrom("A");
        assertEquals(5, reachable.size());
        assertEquals("A", reachable.get(0));
        assertEquals("E", reachable.get(4));
    }

    @Test
    void reachableFromUnknownReturnsEmpty() {
        ServiceDependencyGraph graph = new ServiceDependencyGraph();
        assertTrue(graph.reachableFrom("UNKNOWN").isEmpty());
    }

    @Test
    void inDegreeAndOutDegree() {
        ServiceDependencyGraph graph = new ServiceDependencyGraph();
        graph.addDependency("A", "B");
        graph.addDependency("A", "C");
        graph.addDependency("D", "B");

        assertEquals(2, graph.outDegree("A"));
        assertEquals(0, graph.inDegree("A"));
        assertEquals(2, graph.inDegree("B"));
        assertEquals(0, graph.outDegree("B"));
    }
}