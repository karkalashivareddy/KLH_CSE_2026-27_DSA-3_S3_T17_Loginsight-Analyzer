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
 * Catalogue and module metadata endpoints (docs/REBUILD_BASELINE Phase-2).
 */
@SpringBootTest
@AutoConfigureMockMvc
class CatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void modulesListsSixCourseModules() throws Exception {
        mockMvc.perform(get("/api/modules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].id").value("strings"))
                .andExpect(jsonPath("$[0].algorithmCount").value(9))
                .andExpect(jsonPath("$[0].trackableCount").value(4))
                .andExpect(jsonPath("$[3].id").value("approximation"));
    }

    @Test
    void moduleById() throws Exception {
        mockMvc.perform(get("/api/modules/flow"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("flow"))
                .andExpect(jsonPath("$.algorithms[0].key").value("fordfulkerson"));
        mockMvc.perform(get("/api/modules/nope"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void algorithmByKey() throws Exception {
        mockMvc.perform(get("/api/algorithms/kmp"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value("kmp"))
                .andExpect(jsonPath("$.exposed").value(true))
                .andExpect(jsonPath("$.defaultInput.pattern").value("ABABC"));
        mockMvc.perform(get("/api/algorithms/missing"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void libraryOnlyAlgorithmIsNotExposed() throws Exception {
        mockMvc.perform(get("/api/algorithms/knapsack_fptas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exposed").value(false))
                .andExpect(jsonPath("$.canonicalEndpoint").doesNotExist());
    }
}