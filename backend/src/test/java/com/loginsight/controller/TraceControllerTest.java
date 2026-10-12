package com.loginsight.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
 * Algorithm-laboratory trace endpoints (Phase 12). Each call runs the real instrumented algorithm
 * and must return ordered steps together with the canonical answer, so the assertions cover both
 * the result (matches the ordinary engine) and the presence/shape of the step trace.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TraceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void catalogListsAllLaboratoryAlgorithms() throws Exception {
        mockMvc.perform(get("/api/trace/catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(13))
                .andExpect(jsonPath("$[0].key").exists())
                .andExpect(jsonPath("$[0].category").exists())
                .andExpect(jsonPath("$[0].endpoint").exists());
    }

    @Test
    void naiveTrace() throws Exception {
        mockMvc.perform(post("/api/trace/search/naive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"abracadabra\",\"pattern\":\"abra\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.matchCount").value(2))
                .andExpect(jsonPath("$.steps").isArray())
                .andExpect(jsonPath("$.steps[0].operation").exists());
    }

    @Test
    void kmpTrace() throws Exception {
        mockMvc.perform(post("/api/trace/search/kmp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"abracadabra\",\"pattern\":\"abra\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.matchCount").value(2))
                .andExpect(jsonPath("$.intermediateData.lps").isArray())
                .andExpect(jsonPath("$.steps").isArray());
    }

    @Test
    void zTrace() throws Exception {
        mockMvc.perform(post("/api/trace/search/z")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"aabxaabx\",\"pattern\":\"aabx\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.matchCount").value(2))
                .andExpect(jsonPath("$.steps").isArray());
    }

    @Test
    void rabinKarpTrace() throws Exception {
        mockMvc.perform(post("/api/trace/search/rabin-karp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"abracadabra\",\"pattern\":\"abra\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.matchCount").value(2))
                .andExpect(jsonPath("$.steps").isArray());
    }

    @Test
    void levenshteinTrace() throws Exception {
        mockMvc.perform(post("/api/trace/dp/levenshtein")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"a\":\"kitten\",\"b\":\"sitting\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.distance").value(3))
                .andExpect(jsonPath("$.intermediateData.matrix").isArray())
                .andExpect(jsonPath("$.steps").isArray());
    }

    @Test
    void matrixChainTrace() throws Exception {
        mockMvc.perform(post("/api/trace/dp/matrix-chain")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dims\":[10,20,30,40]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.minCost").value(18000))
                .andExpect(jsonPath("$.intermediateData.m").isArray())
                .andExpect(jsonPath("$.steps").isArray());
    }

    @Test
    void flowTraces() throws Exception {
        String network = "{"
                + "\"source\":\"s\",\"sink\":\"t\","
                + "\"nodes\":[\"s\",\"a\",\"b\",\"t\"],"
                + "\"edges\":[{\"from\":\"s\",\"to\":\"a\",\"capacity\":3},"
                + "{\"from\":\"a\",\"to\":\"t\",\"capacity\":2},"
                + "{\"from\":\"s\",\"to\":\"b\",\"capacity\":1},"
                + "{\"from\":\"b\",\"to\":\"t\",\"capacity\":3}]}";
        mockMvc.perform(post("/api/trace/flow/ford-fulkerson")
                        .contentType(MediaType.APPLICATION_JSON).content(network))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.maxFlow").value(3))
                .andExpect(jsonPath("$.steps").isArray())
                .andExpect(jsonPath("$.steps[?(@.operation=='AUGMENT')]").isNotEmpty());
        mockMvc.perform(post("/api/trace/flow/edmonds-karp")
                        .contentType(MediaType.APPLICATION_JSON).content(network))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.maxFlow").value(3));
        mockMvc.perform(post("/api/trace/flow/dinic")
                        .contentType(MediaType.APPLICATION_JSON).content(network))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.maxFlow").value(3));
    }

    @Test
    void vertexCoverTrace() throws Exception {
        mockMvc.perform(post("/api/trace/approx/vertex-cover")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nodes\":[\"api\",\"auth\",\"db\",\"cache\"],"
                                + "\"edges\":[[\"api\",\"auth\"],[\"api\",\"db\"],"
                                + "[\"auth\",\"db\"],[\"db\",\"cache\"]]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.coverSize").isNumber())
                .andExpect(jsonPath("$.steps").isArray());
    }

    @Test
    void quicksortTrace() throws Exception {
        mockMvc.perform(post("/api/trace/random/quicksort")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"values\":[5,1,4,2,8],\"seed\":7}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.sorted").exists())
                .andExpect(jsonPath("$.steps").isArray());
    }

    @Test
    void millerRabinTrace() throws Exception {
        mockMvc.perform(post("/api/trace/random/prime")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"n\":97}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.prime").value(true))
                .andExpect(jsonPath("$.steps").isArray());
    }

    @Test
    void reservoirTrace() throws Exception {
        mockMvc.perform(post("/api/trace/random/sample")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"k\":3,\"values\":[1,2,3,4,5,6,7,8,9,10],\"seed\":11}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.sample.length()").value(3))
                .andExpect(jsonPath("$.steps").isArray());
    }
}