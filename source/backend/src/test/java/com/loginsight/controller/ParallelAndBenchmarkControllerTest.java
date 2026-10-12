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
import com.loginsight.support.SampleDataGenerator;

/**
 * Parallel engines and the benchmark sweep (docs/12 §8, §10). The reduce scenario runs over the
 * loaded dataset (ERROR_COUNT marker), the scan/sort scenarios are size-driven, and the benchmark
 * returns the measured sequential-vs-parallel table.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ParallelAndBenchmarkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DatasetService datasetService;

    @BeforeEach
    void loadDataset() {
        datasetService.loadSample(SampleDataGenerator.LOGS_SMALL);
    }

    @Test
    void parallelReduce() throws Exception {
        mockMvc.perform(post("/api/parallel/reduce")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"op\":\"ERROR_COUNT\",\"size\":256,\"parallelism\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.algorithm").value("PARALLEL_REDUCE"))
                .andExpect(jsonPath("$.result.verified").value(true))
                .andExpect(jsonPath("$.result.result").isNumber());
    }

    @Test
    void parallelScan() throws Exception {
        mockMvc.perform(post("/api/parallel/scan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"size\":64,\"parallelism\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.algorithm").value("PARALLEL_PREFIX_SCAN"))
                .andExpect(jsonPath("$.result.verified").value(true))
                .andExpect(jsonPath("$.result.last").isNumber());
    }

    @Test
    void parallelSort() throws Exception {
        mockMvc.perform(post("/api/parallel/sort")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"size\":2048,\"parallelism\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.algorithm").value("PARALLEL_SORT"))
                .andExpect(jsonPath("$.result.verified").value(true));
    }

    @Test
    void benchmarkScenario() throws Exception {
        mockMvc.perform(post("/api/benchmark/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scenario\":\"reduce\",\"sizes\":[1000,10000],"
                                + "\"repetitions\":1,\"parallelism\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].algorithm").isNotEmpty())
                .andExpect(jsonPath("$[0].inputSize").value(1000))
                .andExpect(jsonPath("$[0].parallelNanos").isNumber());
    }

    @Test
    void benchmarkRejectsUnknownScenario() throws Exception {
        mockMvc.perform(post("/api/benchmark/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scenario\":\"bogus\",\"sizes\":[1000],\"repetitions\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("InvalidQueryException"));
    }

    @Test
    void benchmarkRejectsAnUnboundedSweep() throws Exception {
        StringBuilder sizes = new StringBuilder("[");
        for (int i = 0; i < 40; i++) {
            sizes.append(i == 0 ? "" : ",").append(1000);
        }
        sizes.append("]");
        mockMvc.perform(post("/api/benchmark/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scenario\":\"reduce\",\"sizes\":" + sizes + ",\"repetitions\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("InvalidQueryException"));
        mockMvc.perform(post("/api/benchmark/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scenario\":\"reduce\",\"sizes\":[5000000],\"repetitions\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("InvalidQueryException"));
    }

    @Test
    void parallelismIsClampedToTheMachineCap() throws Exception {
        mockMvc.perform(post("/api/parallel/scan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"size\":512,\"parallelism\":1000000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.verified").value(true))
                .andExpect(jsonPath("$.result.size").value(512));
        mockMvc.perform(post("/api/parallel/reduce")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"op\":\"SUM\",\"size\":256,\"parallelism\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("InvalidQueryException"));
    }
}