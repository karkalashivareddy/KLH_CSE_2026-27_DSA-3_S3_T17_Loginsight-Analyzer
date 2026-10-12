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
 * The DP and document-similarity endpoints (docs/12 §4): edit distances, Needleman-Wunsch /
 * Smith-Waterman alignment, interval and bitmask DP, tree DP and sum-over-subsets. Payloads are
 * crafted canonical scenarios so the expected numeric answers are stable.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DpControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void levenshteinDistance() throws Exception {
        mockMvc.perform(post("/api/dp/levenshtein")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"a\":\"kitten\",\"b\":\"sitting\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.algorithm").value("LEVENSHTEIN"))
                .andExpect(jsonPath("$.result.distance").value(3));
    }

    @Test
    void damerauDistance() throws Exception {
        mockMvc.perform(post("/api/dp/damerau")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"a\":\"ca\",\"b\":\"ac\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.distance").value(1));
    }

    @Test
    void weightedEditDistance() throws Exception {
        mockMvc.perform(post("/api/dp/weighted-edit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"a\":\"abc\",\"b\":\"ac\",\"insertCost\":2,\"deleteCost\":1,\"substituteCost\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.algorithm").value("WEIGHTED_EDIT_DISTANCE"))
                .andExpect(jsonPath("$.result.distance").value(1));
    }

    @Test
    void globalAndLocalAlignment() throws Exception {
        mockMvc.perform(post("/api/dp/global")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"a\":\"GATTACA\",\"b\":\"GCAT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.algorithm").value("NEEDLEMAN_WUNSCH"))
                .andExpect(jsonPath("$.result.alignedA").isArray())
                .andExpect(jsonPath("$.result.score").isNumber());
        mockMvc.perform(post("/api/dp/local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"a\":\"xabcy\",\"b\":\"abc\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.algorithm").value("SMITH_WATERMAN"))
                .andExpect(jsonPath("$.result.score").value(3));
    }

    @Test
    void matrixChainOrdering() throws Exception {
        mockMvc.perform(post("/api/dp/matrix-chain")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dims\":[10,20,30,40]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.algorithm").value("MATRIX_CHAIN"))
                .andExpect(jsonPath("$.result.minCost").value(18000));
    }

    @Test
    void optimalBst() throws Exception {
        mockMvc.perform(post("/api/dp/obst")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"freqs\":[2,3,1,4]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.algorithm").value("OPTIMAL_BINARY_SEARCH_TREE"))
                .andExpect(jsonPath("$.result.optimalCost").isNumber());
    }

    @Test
    void tspHamiltonianTreeSos() throws Exception {
        mockMvc.perform(post("/api/dp/tsp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"costs\":[[0,10,15],[10,0,35],[15,35,0]],\"start\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.algorithm").value("BITMASK_TSP"))
                .andExpect(jsonPath("$.result.minCost").value(60));
        mockMvc.perform(post("/api/dp/hamiltonian")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":[0,1,2,3],\"to\":[1,2,3,0],\"start\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.exists").value(true));
        mockMvc.perform(post("/api/dp/tree")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":[0,0,1],\"to\":[1,2,3]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.algorithm").value("TREE_DIAMETER"));
        mockMvc.perform(post("/api/dp/sos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"values\":[1,2,3,4]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.algorithm").value("SOS_DP"))
                .andExpect(jsonPath("$.result.subsetSums[3]").value(10));
    }

    @Test
    void quadraticDpRejectsOversizedSequences() throws Exception {
        String huge = "x".repeat(6_000);
        mockMvc.perform(post("/api/dp/levenshtein")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"a\":\"" + huge + "\",\"b\":\"" + huge + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("InvalidQueryException"));
        mockMvc.perform(post("/api/dp/global")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"a\":\"" + huge + "\",\"b\":\"" + huge + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("InvalidQueryException"));
        mockMvc.perform(post("/api/dp/local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"a\":\"" + huge + "\",\"b\":\"" + huge + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("InvalidQueryException"));
        mockMvc.perform(post("/api/dp/damerau")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"a\":\"" + huge + "\",\"b\":\"" + huge + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("InvalidQueryException"));
    }
}