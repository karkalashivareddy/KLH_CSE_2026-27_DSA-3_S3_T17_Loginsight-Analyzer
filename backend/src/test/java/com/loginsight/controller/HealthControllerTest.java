package com.loginsight.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Liveness and readiness surface (docs/12 §9): the base probe stays stable, status reports the
 * engine registry, and the dataset probe flips from 404 to 200 once a dataset is loaded.
 */
@SpringBootTest
@AutoConfigureMockMvc
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.loginsight.service.DatasetService datasetService;

    @Test
    void healthReturnsUp() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("loginsight-analyzer"));
    }

    @Test
    void statusReportsEngineCount() throws Exception {
        datasetService.clear();
        mockMvc.perform(get("/api/health/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.datasetLoaded").value(false))
                .andExpect(jsonPath("$.engines").isNumber());
    }

    @Test
    void datasetProbeRequiresLoad() throws Exception {
        datasetService.clear();
        mockMvc.perform(get("/api/health/dataset")).andExpect(status().isNotFound());
    }

    @Test
    void readyReturnsUp() throws Exception {
        mockMvc.perform(get("/api/health/ready"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.ready").value(true));
    }
}