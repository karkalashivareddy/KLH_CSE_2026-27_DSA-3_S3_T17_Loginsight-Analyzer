package com.loginsight.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The Algorithm Lab run path: {@code GET /api/analysis/algorithms} must publish the catalogue
 * default input next to the metadata it already returned, and that exact payload must be accepted
 * by {@code POST /api/runs} so the created session can be replayed over SSE. Nothing here
 * synthesises an input: the body posted to the runs API is the JSON the catalogue endpoint returned.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AlgorithmGroupRunPathTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Test
    void algorithmGroupsExposeTheCatalogueDefaultInputAlongsideExistingMetadata() throws Exception {
        mockMvc.perform(get("/api/analysis/algorithms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].module").value("Strings"))
                .andExpect(jsonPath("$[0].algorithms[0].key").value("naive"))
                .andExpect(jsonPath("$[0].algorithms[0].name").value("Naive Pattern Search"))
                .andExpect(jsonPath("$[0].algorithms[0].queryType").value("PATTERN_SEARCH"))
                .andExpect(jsonPath("$[0].algorithms[0].algorithmType").value("NAIVE"))
                .andExpect(jsonPath("$[0].algorithms[0].canonicalEndpoint")
                        .value("/api/search/naive"))
                .andExpect(jsonPath("$[0].algorithms[0].traceEndpoint")
                        .value("/api/trace/search/naive"))
                .andExpect(jsonPath("$[0].algorithms[0].timeComplexity").value("O(n·m)"))
                .andExpect(jsonPath("$[0].algorithms[0].spaceComplexity").value("O(1)"))
                .andExpect(jsonPath("$[0].algorithms[0].tracked").value(true))
                .andExpect(jsonPath("$[0].algorithms[0].defaultInput.pattern").value("ABABC"))
                .andExpect(jsonPath("$[0].algorithms[0].description").isNotEmpty());
    }

    @Test
    void untraceableLibraryEntryPublishesNoRunInput() throws Exception {
        Map<String, Object> item = groupItem("perfect_hash");

        assertEquals("Two-Level Perfect Hashing", item.get("name"));
        assertEquals(Boolean.FALSE, item.get("tracked"));
        assertTrue(item.containsKey("defaultInput"), "the field is always present");
        assertNull(item.get("defaultInput"), "library-only entries carry no runnable input");
    }

    @Test
    void everyTrackedCatalogueInputCreatesAReplayableRunSession() throws Exception {
        List<Map<String, Object>> tracked = groupItems().stream()
                .filter(item -> Boolean.TRUE.equals(item.get("tracked")))
                .toList();
        assertEquals(13, tracked.size(), "expected the 13 trace-instrumented algorithms");

        for (Map<String, Object> item : tracked) {
            String key = String.valueOf(item.get("key"));
            Object defaultInput = item.get("defaultInput");
            assertNotNull(defaultInput, key + " is traceable and must publish a default input");

            String body = mockMvc.perform(post("/api/runs")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(runPayload(key, defaultInput)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.algorithm").value(key))
                    .andExpect(jsonPath("$.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.stepCount").value(greaterThan(0)))
                    .andExpect(jsonPath("$.truncated").value(false))
                    .andReturn().getResponse().getContentAsString();

            mockMvc.perform(get("/api/runs/{id}", textField(body, "runId")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.input").exists());
        }
    }

    @Test
    void createdRunSessionReplaysRecordedStepsOverSse() throws Exception {
        Object defaultInput = groupItem("matrixchain").get("defaultInput");
        String body = mockMvc.perform(post("/api/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(runPayload("matrixchain", defaultInput)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andReturn().getResponse().getContentAsString();

        MvcResult async = mockMvc.perform(get("/api/runs/{id}/events", textField(body, "runId")))
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(async))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("event:meta")))
                .andExpect(content().string(containsString("event:step")))
                .andExpect(content().string(containsString("event:complete")));
    }

    @Test
    void auditedMetadataReachesTheAlgorithmGroupsApi() throws Exception {
        Map<String, Object> hamiltonian = groupItem("hamiltonian");
        assertEquals("Hamiltonian Path via Bitmask DP", hamiltonian.get("name"));
        assertEquals("Existence of a Hamiltonian path", hamiltonian.get("problem"));
        assertEquals("BITMASK_DP", hamiltonian.get("queryType"));
        assertEquals("HAMILTONIAN_PATH", hamiltonian.get("algorithmType"));
        assertEquals("O(2^n·n²)", hamiltonian.get("timeComplexity"));
        assertEquals("O(2^n·n)", hamiltonian.get("spaceComplexity"));

        Map<String, Object> incidentCover = groupItem("incident_cover");
        assertEquals("APPROXIMATE_COVER", incidentCover.get("queryType"));
        assertEquals("MAXIMAL_MATCHING", incidentCover.get("algorithmType"));

        Map<String, Object> parallelScan = groupItem("parallel_scan");
        assertEquals("PARALLEL_SCAN", parallelScan.get("queryType"));
        assertEquals("PARALLEL_PREFIX_SCAN", parallelScan.get("algorithmType"));

        assertEquals("O(V·E)", groupItem("vertex_cover_kernelization").get("timeComplexity"));
        assertEquals("O(line · |q|) DP table", groupItem("fuzzy_search").get("spaceComplexity"));
    }

    // ------------------------------------------------------------------ helpers

    private String runPayload(String key, Object defaultInput) throws Exception {
        return mapper.writeValueAsString(Map.of("algorithm", key, "input", defaultInput));
    }

    private Map<String, Object> groupItem(String key) throws Exception {
        for (Map<String, Object> item : groupItems()) {
            if (key.equals(item.get("key"))) {
                return item;
            }
        }
        throw new IllegalStateException("algorithm group response has no entry " + key);
    }

    private List<Map<String, Object>> groupItems() throws Exception {
        String body = mockMvc.perform(get("/api/analysis/algorithms"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Map<String, Object>> groups = mapper.readValue(body, new TypeReference<>() {
        });
        List<Map<String, Object>> items = new ArrayList<>();
        for (Map<String, Object> group : groups) {
            items.addAll(asItems(group.get("algorithms")));
        }
        return items;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> asItems(Object algorithms) {
        return (List<Map<String, Object>>) algorithms;
    }

    private String textField(String body, String name) throws Exception {
        Object value = mapper.readValue(body, new TypeReference<Map<String, Object>>() {
        }).get(name);
        assertNotNull(value, "response has no " + name);
        return value.toString();
    }
}
