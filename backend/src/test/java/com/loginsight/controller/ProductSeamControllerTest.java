package com.loginsight.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import com.loginsight.service.DatasetService;

@SpringBootTest
@AutoConfigureMockMvc
class ProductSeamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DatasetService datasetService;

    @BeforeEach
    void loadDataset() {
        datasetService.loadDemo();
    }

    @Test
    void analyticsRejectsUnboundedQueryValues() throws Exception {
        mockMvc.perform(get("/api/analytics/errors").param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("InvalidQueryException"));
        mockMvc.perform(get("/api/analytics/top").param("limit", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("InvalidQueryException"));
        mockMvc.perform(get("/api/analytics/windows").param("buckets", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("InvalidQueryException"));
        mockMvc.perform(get("/api/analytics/hosts").param("limit", "201"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("InvalidQueryException"));
    }

    @Test
    void analyticsRejectsUnknownDimensions() throws Exception {
        mockMvc.perform(get("/api/analytics/top").param("dimension", "host"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("InvalidQueryException"));
    }

    @Test
    void patternsAndServicesRejectUnboundedQueryValues() throws Exception {
        mockMvc.perform(get("/api/patterns").param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("InvalidQueryException"));
        mockMvc.perform(get("/api/patterns/examples")
                        .param("template", "request <*>")
                        .param("limit", "201"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("InvalidQueryException"));
        mockMvc.perform(get("/api/patterns").param("level", "unknown"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("InvalidQueryException"));
        mockMvc.perform(get("/api/services").param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("InvalidQueryException"));
        mockMvc.perform(get("/api/services/api-gateway").param("recent", "501"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("InvalidQueryException"));
    }

    @Test
    void liveRejectsOutOfRangeReplayControls() throws Exception {
        mockMvc.perform(get("/api/live").param("batchSize", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("InvalidQueryException"));
        mockMvc.perform(get("/api/live").param("intervalMs", "99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("InvalidQueryException"));
        mockMvc.perform(get("/api/live").param("intervalMs", "60001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("InvalidQueryException"));
    }
}
