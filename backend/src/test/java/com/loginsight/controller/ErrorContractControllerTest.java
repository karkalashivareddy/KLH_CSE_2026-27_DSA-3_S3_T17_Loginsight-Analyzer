package com.loginsight.controller;

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

/**
 * Error contract (docs/12 §12): with no dataset loaded the dataset-scoped searches return 404 with
 * the documented JSON shape; malformed requests return 400; unknown algorithm ids and unsupported
 * keys are never 500s.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ErrorContractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DatasetService datasetService;

    @BeforeEach
    void clearDataset() {
        datasetService.clear();
    }

    @Test
    void datasetScopedSearchWithoutDatasetIs404() throws Exception {
        mockMvc.perform(post("/api/search/naive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pattern\":\"ERROR\",\"scope\":\"DATASET\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("DatasetException"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/search/naive"));
    }

    @Test
    void blankPatternIs400() throws Exception {
        mockMvc.perform(post("/api/search/naive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pattern\":\"   \",\"text\":\"hello world\",\"scope\":\"EXPLICIT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("InvalidQueryException"))
                .andExpect(jsonPath("$.message").value("pattern must not be blank"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void missingBodyFieldsIs400() throws Exception {
        mockMvc.perform(post("/api/dp/levenshtein")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"a\":\"kitten\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("InvalidQueryException"));
    }

    @Test
    void unknownAlgorithmKeyIs400() throws Exception {
        mockMvc.perform(post("/api/random/sample")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"k\":5,\"size\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("InvalidQueryException"));
    }
}