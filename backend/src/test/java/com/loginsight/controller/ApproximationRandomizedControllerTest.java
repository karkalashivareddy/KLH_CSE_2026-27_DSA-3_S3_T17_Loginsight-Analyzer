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
 * Approximation and randomized endpoints (docs/12 §5, §7). Canonical inputs assert stable outputs,
 * including the greedy vertex-cover ratio notes and Miller-Rabin primality classification.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApproximationRandomizedControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void vertexCover() throws Exception {
        mockMvc.perform(post("/api/approx/vertex-cover")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nodes\":[\"A\",\"B\",\"C\"],"
                                + "\"edges\":[[\"A\",\"B\"],[\"B\",\"C\"]]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.algorithm").value("VERTEX_COVER"))
                .andExpect(jsonPath("$.result.coverSize").value(2))
                .andExpect(jsonPath("$.result.approximationRatio").value(2.0));
    }

    @Test
    void setCover() throws Exception {
        mockMvc.perform(post("/api/approx/set-cover")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"universe\":[\"1\",\"2\",\"3\",\"4\",\"5\"],"
                                + "\"sets\":{\"A\":[\"1\",\"2\",\"3\"],\"B\":[\"3\",\"4\"],"
                                + "\"C\":[\"4\",\"5\"]}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.allCovered").value(true))
                .andExpect(jsonPath("$.result.selectedSets").isArray());
    }

    @Test
    void incidentCover() throws Exception {
        mockMvc.perform(post("/api/approx/incident-cover")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"services\":[\"S1\",\"S2\",\"S3\"],"
                                + "\"relationships\":[[\"S1\",\"S2\"],[\"S2\",\"S3\"]]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.coverSize").value(2));
    }

    @Test
    void millerRabinPrimality() throws Exception {
        mockMvc.perform(post("/api/random/prime")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"n\":101,\"rounds\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.algorithm").value("MILLER_RABIN"))
                .andExpect(jsonPath("$.result.isPrime").value(true));
        mockMvc.perform(post("/api/random/prime")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"n\":100,\"rounds\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.isPrime").value(false));
    }

    @Test
    void reservoirSample() throws Exception {
        mockMvc.perform(post("/api/random/sample")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"k\":5,\"size\":1000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.k").value(5))
                .andExpect(jsonPath("$.result.sample.length()").value(5));
    }

    @Test
    void universalHash() throws Exception {
        mockMvc.perform(post("/api/random/hash")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"hello\",\"m\":16}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.algorithm").value("UNIVERSAL_HASH"))
                .andExpect(jsonPath("$.result.hashValue").isNumber())
                .andExpect(jsonPath("$.result.a").isNumber());
    }

    @Test
    void randomizedQuicksort() throws Exception {
        mockMvc.perform(post("/api/random/quicksort")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"values\":[3,1,2,5,4]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.algorithm").value("RANDOMIZED_QUICKSORT"))
                .andExpect(jsonPath("$.result.verifiedSorted").value(true))
                .andExpect(jsonPath("$.result.sorted[0]").value(1))
                .andExpect(jsonPath("$.result.sorted[4]").value(5));
    }
}