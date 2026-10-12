package com.loginsight.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Flow-family endpoints (docs/12 §3): three max-flow algorithms on the same canonical network, the
 * matching split and the min-cost shipment. The expected pipe values are stable for these graphs.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FlowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String NETWORK = "{"
            + "\"source\":\"s\",\"sink\":\"t\","
            + "\"nodes\":[\"s\",\"a\",\"b\",\"t\"],"
            + "\"edges\":[{\"from\":\"s\",\"to\":\"a\",\"capacity\":3},"
            + "{\"from\":\"a\",\"to\":\"t\",\"capacity\":2},"
            + "{\"from\":\"s\",\"to\":\"b\",\"capacity\":1},"
            + "{\"from\":\"b\",\"to\":\"t\",\"capacity\":3}]}";

    @Test
    void fordFulkersonMaxFlow() throws Exception {
        mockMvc.perform(post("/api/flow")
                        .contentType(MediaType.APPLICATION_JSON).content(NETWORK))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.algorithm").value("FORD_FULKERSON"))
                .andExpect(jsonPath("$.result.maxFlow").value(3))
                .andExpect(jsonPath("$.result.augmentingPaths").value(2));
    }

    @Test
    void edmondsKarpAndDinicAgree() throws Exception {
        mockMvc.perform(post("/api/flow/edmonds-karp")
                        .contentType(MediaType.APPLICATION_JSON).content(NETWORK))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.maxFlow").value(3));
        mockMvc.perform(post("/api/flow/dinic")
                        .contentType(MediaType.APPLICATION_JSON).content(NETWORK))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.algorithm").value("DINIC"))
                .andExpect(jsonPath("$.result.maxFlow").value(3));
    }

    @Test
    void minCutEqualsMaxFlow() throws Exception {
        mockMvc.perform(post("/api/flow/min-cut")
                        .contentType(MediaType.APPLICATION_JSON).content(NETWORK))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.cutCapacity").value(3))
                .andExpect(jsonPath("$.result.sourceSide").isArray());
    }

    @Test
    void bipartiteMatching() throws Exception {
        mockMvc.perform(post("/api/flow/matching")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"incidents\":[\"inc1\",\"inc2\"],"
                                + "\"resources\":[\"res1\",\"res2\",\"res3\"],"
                                + "\"edges\":[[\"inc1\",\"res1\"],[\"inc2\",\"res1\"],[\"inc2\",\"res2\"]]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.algorithm").value("BIPARTITE_MATCHING"))
                .andExpect(jsonPath("$.result.matchingSize").value(2));
    }

    @Test
    void minCostFlow() throws Exception {
        mockMvc.perform(post("/api/flow/min-cost")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"suppliers\":[\"s1\",\"s2\"],\"demand\":[\"c1\",\"c2\"],"
                                + "\"costEdges\":[[\"s1\",\"c1\",\"5\"],[\"s1\",\"c2\",\"2\"],"
                                + "[\"s2\",\"c1\",\"1\"],[\"s2\",\"c2\",\"8\"]]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalFlow").value(2))
                .andExpect(jsonPath("$.result.totalCost").value(3));
    }
}