package com.loginsight.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Error contract for {@code POST /api/datasets} uploads: an upload that carries no parsable content
 * is a client mistake and resolves to the standard 400 envelope with the parser's own message.
 *
 * <p>Before {@code ParserException} was mapped, the same request fell through to the generic
 * {@code Exception} handler and answered HTTP 500.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class DatasetUploadErrorContractTest {

    @Autowired
    private MockMvc mockMvc;

    private static MockMultipartFile upload(String name, String content) {
        return new MockMultipartFile("file", name, "text/plain",
                content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test
    void zeroByteUploadIs400Not500Or404() throws Exception {
        mockMvc.perform(multipart("/api/datasets").file(upload("empty.log", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("UnsupportedLogFormatException"))
                .andExpect(jsonPath("$.message").value("input stream is empty"))
                .andExpect(jsonPath("$.path").value("/api/datasets"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void blankOnlyUploadIs400Not500Or404() throws Exception {
        mockMvc.perform(multipart("/api/datasets").file(upload("blank.log", "\n\n   \n")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("UnsupportedLogFormatException"));
    }

    @Test
    void unparsableUploadAnswersTheStandardEnvelopeWithoutAStackTrace() throws Exception {
        String body = mockMvc.perform(multipart("/api/datasets")
                        .file(upload("garbage.log", "this is not a log line at all\n")))
                .andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertFalse(body.contains("Exception in thread"));
        org.junit.jupiter.api.Assertions.assertFalse(body.contains("\\tat "));
        org.junit.jupiter.api.Assertions.assertFalse(body.contains("com.loginsight"));
    }

    @Test
    void validUploadStillSucceeds() throws Exception {
        String jsonl = "{\"timestamp\":\"2026-09-13T10:00:01Z\",\"level\":\"ERROR\","
                + "\"message\":\"boom\"}\n";
        mockMvc.perform(multipart("/api/datasets")
                        .file(upload("ok.jsonl", jsonl))
                        .param("name", "regression-upload"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datasetName").value("regression-upload"))
                .andExpect(jsonPath("$.size").value(1));
    }
}