package com.loginsight.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.loginsight.service.text.TextHackService;

/**
 * TextHack unified query endpoint (docs/REBUILD_BASELINE Phase-3). {@code queryClass} selects one of
 * six course-scoped classes; {@code input} mirrors the request DTO of the underlying engine.
 */
@RestController
@RequestMapping("/api")
public class TextHackController {

    private final TextHackService textHack;

    public TextHackController(TextHackService textHack) {
        this.textHack = textHack;
    }

    @PostMapping("/text-hack/query")
    public ResponseEntity<?> query(@RequestBody Map<String, Object> body) {
        String queryClass = body.get("queryClass") instanceof String raw ? raw
                : body.get("queryClass") == null ? null
                        : String.valueOf(body.get("queryClass"));
        @SuppressWarnings("unchecked")
        Map<String, Object> input = body.get("input") instanceof Map<?, ?> map
                ? (Map<String, Object>) map : Map.of();
        return ResponseEntity.ok(textHack.execute(queryClass, input));
    }
}