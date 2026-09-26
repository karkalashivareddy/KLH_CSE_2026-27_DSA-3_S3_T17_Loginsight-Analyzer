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

    @Test
    void malformedJsonBodyIs400Not500() throws Exception {
        mockMvc.perform(post("/api/dp/levenshtein")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"a\":\"kitten\", "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("HttpMessageNotReadableException"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/dp/levenshtein"));
    }

    @Test
    void recordValidationFailureIs400Not500() throws Exception {
        mockMvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"x\",\"size\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void mistypedPathVariableIs400() throws Exception {
        datasetService.loadDemo();
        mockMvc.perform(get("/api/logs/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentTypeMismatchException"))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("id")));
    }

    @Test
    void mistypedRequestParamIs400() throws Exception {
        datasetService.loadDemo();
        mockMvc.perform(get("/api/logs").param("limit", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentTypeMismatchException"));
    }

    @Test
    void unparsableDateParamIs400() throws Exception {
        datasetService.loadDemo();
        mockMvc.perform(get("/api/logs/explore").param("q", "").param("from", "13-09-2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentTypeMismatchException"))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("from")));
    }

    @Test
    void invertedDateRangeIs400() throws Exception {
        datasetService.loadDemo();
        mockMvc.perform(get("/api/logs/explore")
                        .param("from", "2026-09-13T12:00:00Z")
                        .param("to", "2026-09-13T10:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("InvalidQueryException"));
    }

    @Test
    void unsupportedMethodIs405() throws Exception {
        mockMvc.perform(get("/api/dp/levenshtein"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.error").value("HttpRequestMethodNotSupportedException"));
    }

    @Test
    void missingRequestParamIs400() throws Exception {
        mockMvc.perform(get("/api/analysis/benchmarks/search"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("MissingServletRequestParameterException"))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("pattern")));
    }

    @Test
    void unknownApiRouteIs404() throws Exception {
        mockMvc.perform(get("/api/definitely-not-a-route"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void errorBodiesNeverCarryAStackTrace() throws Exception {
        String body = mockMvc.perform(post("/api/dp/levenshtein")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"a\":"))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertFalse(body.contains("Exception in thread"));
        org.junit.jupiter.api.Assertions.assertFalse(body.contains("\\tat "));
        org.junit.jupiter.api.Assertions.assertFalse(body.contains("com.loginsight"));
    }
}