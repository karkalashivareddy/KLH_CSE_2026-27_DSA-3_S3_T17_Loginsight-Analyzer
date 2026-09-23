package com.loginsight.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Run lifecycle and SSE replay (docs/REBUILD_BASELINE Phase-4): a run records genuine steps, the
 * history is bounded and the event stream replays them as meta/step/complete events.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RunControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createListGetAndReplay() throws Exception {
        String runId = createKmpRun();

        mockMvc.perform(get("/api/runs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].runId").value(runId))
                .andExpect(jsonPath("$[0].algorithm").value("kmp"))
                .andExpect(jsonPath("$[0].stepCount").value(greaterThan(0)));

        mockMvc.perform(get("/api/runs/{id}", runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.steps[0].operation").exists())
                .andExpect(jsonPath("$.truncated").value(false));

        mockMvc.perform(get("/api/runs/{id}/result", runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.result.matchCount").exists());

        MvcResult async = mockMvc.perform(get("/api/runs/{id}/events", runId))
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(async))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("event:meta")))
                .andExpect(content().string(containsString("event:step")))
                .andExpect(content().string(containsString("event:complete")));
    }

    @Test
    void failedRunIsRecordedAsFailed() throws Exception {
        String body = mockMvc.perform(post("/api/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"algorithm\":\"kmp\",\"input\":{\"text\":\"x\"}}"))
                .andReturn().getResponse().getContentAsString();
        String runId = body.contains("\"runId\":\"")
                ? body.substring(body.indexOf("\"runId\":\"") + 9,
                        body.indexOf('"', body.indexOf("\"runId\":\"") + 9))
                : "";
        mockMvc.perform(get("/api/runs/{id}", runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    void unknownAlgorithmIsRejectedAndUnknownRunIs400() throws Exception {
        mockMvc.perform(post("/api/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"algorithm\":\"dijkstra\",\"input\":{}}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/runs/missing-run"))
                .andExpect(status().isBadRequest());
    }

    private String createKmpRun() throws Exception {
        String response = mockMvc.perform(post("/api/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"algorithm\":\"kmp\",\"input\":{"
                                + "\"pattern\":\"ABABC\",\"text\":\"ABABABC\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andReturn().getResponse().getContentAsString();
        int start = response.indexOf("\"runId\":\"") + 9;
        return response.substring(start, response.indexOf('"', start));
    }
}