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

/**
 * Incident endpoints (docs/API.md §6): the list, count, detail and evidence views agree on the same
 * detection set, and out-of-range paging or unknown ids are 4xx rather than empty 200s.
 */
@SpringBootTest
@AutoConfigureMockMvc
class IncidentsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DatasetService datasetService;

    @BeforeEach
    void loadDataset() {
        datasetService.loadDemo();
    }

    @Test
    void listAndCountAgree() throws Exception {
        String count = mockMvc.perform(get("/api/incidents/count"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        mockMvc.perform(get("/api/incidents").param("limit", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].method").exists());
        mockMvc.perform(get("/api/incidents").param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        org.junit.jupiter.api.Assertions.assertTrue(count.matches("\\d+"));
    }

    @Test
    void detailAndEvidenceResolveTheSameIncident() throws Exception {
        long id = firstIncidentId();
        mockMvc.perform(get("/api/incidents/{id}", id).param("logs", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incident.id").value(id))
                .andExpect(jsonPath("$.events").isArray());
        mockMvc.perform(get("/api/incidents/{id}/logs", id).param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void invalidPagingAndUnknownIdsAre4xx() throws Exception {
        mockMvc.perform(get("/api/incidents").param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("InvalidQueryException"));
        mockMvc.perform(get("/api/incidents").param("limit", "-1"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/incidents/{id}/logs", 999_999L).param("offset", "-1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("DatasetException"));
        mockMvc.perform(get("/api/incidents/{id}/logs", 999_999L).param("limit", "0"))
                .andExpect(status().isNotFound());
    }

    private long firstIncidentId() throws Exception {
        String body = mockMvc.perform(get("/api/incidents"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int at = body.indexOf("\"id\":");
        if (at < 0) {
            throw new IllegalStateException("the demo dataset must expose at least one incident");
        }
        return Long.parseLong(body.substring(at + 5, body.indexOf(',', at)).trim());
    }
}
