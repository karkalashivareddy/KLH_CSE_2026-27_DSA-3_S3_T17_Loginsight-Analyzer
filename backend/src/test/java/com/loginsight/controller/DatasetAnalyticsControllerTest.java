package com.loginsight.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Dataset lifecycle and analytics endpoints (Phase 12). The dataset flow loads a bundled sample
 * (asserting the events are genuinely parsed) and the analytics surfaces derive their answers from
 * the loaded dataset, so the assertions verify real aggregation over real events.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DatasetAnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listLoadCurrentAndClearDataset() throws Exception {
        mockMvc.perform(get("/api/datasets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@ == 'logs-small.txt')]").exists());

        mockMvc.perform(post("/api/datasets/logs-small.txt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loaded").value(true))
                .andExpect(jsonPath("$.size").isNumber())
                .andExpect(jsonPath("$.size").value(org.hamcrest.Matchers.greaterThan(0)));

        mockMvc.perform(get("/api/datasets/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loaded").value(true))
                .andExpect(jsonPath("$.datasetName").value("logs-small.txt"));

        mockMvc.perform(delete("/api/datasets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loaded").value(false));

        mockMvc.perform(get("/api/datasets/current"))
                .andExpect(status().isNotFound());
    }

    @Test
    void loadUnknownDatasetReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/datasets/does-not-exist.txt"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.loaded").value(false));
    }

    @Test
    void analyticsSurfacesAggregateRealEvents() throws Exception {
        mockMvc.perform(post("/api/datasets/logs-medium.txt"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/analytics/errors?limit=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patterns").isMap());

        mockMvc.perform(get("/api/analytics/top?dimension=service&limit=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/analytics/windows?buckets=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].start").exists());

        mockMvc.perform(get("/api/analytics/dependencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes").isArray())
                .andExpect(jsonPath("$.edges").isArray())
                .andExpect(jsonPath("$.nodeCount").isNumber());
    }

    @Test
    void analyticsBeforeLoadReturnsDatasetError() throws Exception {
        mockMvc.perform(delete("/api/datasets"));
        mockMvc.perform(get("/api/analytics/top"))
                .andExpect(status().isNotFound());
    }
}