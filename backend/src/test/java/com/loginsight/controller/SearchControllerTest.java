package com.loginsight.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.loginsight.service.DatasetService;
import com.loginsight.support.SampleDataGenerator;

/**
 * Log explorer and string-search endpoints over a loaded dataset (docs/12 §1, §2). Exercises the
 * dataset scope, the explicit-text scope, the Aho-Corasick multi-pattern pass, suffix analysis and
 * the fuzzy search.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DatasetService datasetService;

    @BeforeEach
    void loadDataset() {
        datasetService.loadSample(SampleDataGenerator.LOGS_SMALL);
    }

    @Test
    void logExplorerEndpoints() throws Exception {
        mockMvc.perform(get("/api/logs/first"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.level").isNotEmpty());
        mockMvc.perform(get("/api/logs").param("limit", "5").param("offset", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));
        mockMvc.perform(get("/api/logs/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLogs").value(1000))
                .andExpect(jsonPath("$.levels.ERROR").isNumber());
    }

    @Test
    void naiveSearchOverDataset() throws Exception {
        mockMvc.perform(post("/api/search/naive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pattern\":\"ERROR\",\"scope\":\"DATASET\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.algorithm").value("NAIVE"))
                .andExpect(jsonPath("$.queryType").value("PATTERN_SEARCH"))
                .andExpect(jsonPath("$.pattern").value("ERROR"))
                .andExpect(jsonPath("$.result.matchCount").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.timeComplexity").isNotEmpty());
    }

    @Test
    void kmpAndZAndRabinKarpAcceptExplicitText() throws Exception {
        String body = "{\"pattern\":\"ana\",\"text\":\"banana\",\"scope\":\"EXPLICIT\"}";
        mockMvc.perform(post("/api/search/kmp").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.matchCount").value(2));
        mockMvc.perform(post("/api/search/z").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.matchCount").value(2));
        mockMvc.perform(post("/api/search/rabin-karp").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.matchCount").value(2));
    }

    @Test
    void multiPatternSearch() throws Exception {
        mockMvc.perform(post("/api/search/multi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patterns\":[\"login\",\"payment\"],\"scope\":\"DATASET\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.algorithm").value("AHO_CORASICK"))
                .andExpect(jsonPath("$.result.occurrences").isArray());
    }

    @Test
    void suffixBuildAndSearch() throws Exception {
        mockMvc.perform(post("/api/string/suffix/build")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"banana\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.length").value(6));
        mockMvc.perform(post("/api/string/suffix/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"banana\",\"pattern\":\"ana\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.matchCount").value(2));
    }

    @Test
    void fuzzySearchOverDataset() throws Exception {
        mockMvc.perform(post("/api/fuzzy/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"request completed\",\"maxDistance\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.algorithm").value("LEVENSHTEIN"))
                .andExpect(jsonPath("$.result.totalMatches").isNumber());
    }

    @Test
    void multiPatternRejectsAnUnboundedPatternSet() throws Exception {
        StringBuilder tooMany = new StringBuilder("[");
        for (int i = 0; i < 2_000; i++) {
            tooMany.append(i == 0 ? "" : ",").append("\"p").append(i).append("\"");
        }
        tooMany.append("]");
        mockMvc.perform(post("/api/search/multi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patterns\":" + tooMany + ",\"text\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("InvalidQueryException"));
    }

    @Test
    void multiPatternRejectsAnOversizedSinglePattern() throws Exception {
        mockMvc.perform(post("/api/search/multi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patterns\":[\"" + "x".repeat(150_000) + "\"],\"text\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("InvalidQueryException"));
    }
}