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
 * TextHack unified query endpoint (docs/REBUILD_BASELINE Phase-3): each query class routes to its
 * real engine and carries honest labels (approximate/expected) through the response.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TextHackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void patternSearchUsesKmp() throws Exception {
        mockMvc.perform(post("/api/text-hack/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"queryClass\":\"PATTERN_SEARCH\","
                                + "\"input\":{\"pattern\":\"ABABC\","
                                + "\"text\":\"ABABABCABABABC\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queryClass").value("PATTERN_SEARCH"))
                .andExpect(jsonPath("$.moduleId").value("strings"))
                .andExpect(jsonPath("$.executed.algorithm").value("KMP"))
                .andExpect(jsonPath("$.executed.result.matchCount").value(2))
                .andExpect(jsonPath("$.traceAlgorithmKey").value("kmp"))
                .andExpect(jsonPath("$.recommended.length()").value(4));
    }

    @Test
    void fuzzyMatchBuildsFromQuery() throws Exception {
        mockMvc.perform(post("/api/text-hack/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"queryClass\":\"FUZZY_MATCH\","
                                + "\"input\":{\"query\":\"kitten\","
                                + "\"text\":\"sitting\\nkitten\\nmittens\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executed.algorithm").value("LEVENSHTEIN"));
    }

    @Test
    void documentSimilarityReportsRealIdentityRatio() throws Exception {
        mockMvc.perform(post("/api/text-hack/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"queryClass\":\"DOCUMENT_SIMILARITY\","
                                + "\"input\":{\"a\":\"kitten\",\"b\":\"sitting\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executed.algorithm").value("NEEDLEMAN_WUNSCH"))
                .andExpect(jsonPath("$.executed.result.similarityPercent").isNumber())
                .andExpect(jsonPath("$.executed.result.alignmentLength").value(7));
    }

    @Test
    void citationFlowUsesDinic() throws Exception {
        mockMvc.perform(post("/api/text-hack/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"queryClass\":\"CITATION_FLOW\",\"input\":{"
                                + "\"source\":\"S\",\"sink\":\"T\","
                                + "\"nodes\":[\"S\",\"A\",\"B\",\"T\"],"
                                + "\"edges\":[{\"from\":\"S\",\"to\":\"A\",\"capacity\":10},"
                                + "{\"from\":\"S\",\"to\":\"B\",\"capacity\":5},"
                                + "{\"from\":\"A\",\"to\":\"B\",\"capacity\":5},"
                                + "{\"from\":\"A\",\"to\":\"T\",\"capacity\":5},"
                                + "{\"from\":\"B\",\"to\":\"T\",\"capacity\":10}]}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executed.algorithm").value("DINIC"))
                .andExpect(jsonPath("$.traceAlgorithmKey").value("dinic"));
    }

    @Test
    void projectSchedulingCoversAllEdges() throws Exception {
        mockMvc.perform(post("/api/text-hack/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"queryClass\":\"PROJECT_SCHEDULING\",\"input\":{"
                                + "\"nodes\":[\"A\",\"B\",\"C\",\"D\"],"
                                + "\"edges\":[[\"A\",\"B\"],[\"B\",\"C\"],[\"C\",\"D\"]]}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executed.algorithm").value("VERTEX_COVER"))
                .andExpect(jsonPath("$.executed.result.coverSize").value(4))
                .andExpect(jsonPath("$.executed.result.approximationRatio").value(2.0));
    }

    @Test
    void primeTestingClassifiesComposite() throws Exception {
        mockMvc.perform(post("/api/text-hack/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"queryClass\":\"PRIME_TESTING\","
                                + "\"input\":{\"n\":121,\"rounds\":10}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executed.algorithm").value("MILLER_RABIN"))
                .andExpect(jsonPath("$.executed.result.isPrime").value(false));
    }

    @Test
    void unknownQueryClassIsRejected() throws Exception {
        mockMvc.perform(post("/api/text-hack/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"queryClass\":\"NONSENSE\",\"input\":{}}"))
                .andExpect(status().isBadRequest());
    }
}