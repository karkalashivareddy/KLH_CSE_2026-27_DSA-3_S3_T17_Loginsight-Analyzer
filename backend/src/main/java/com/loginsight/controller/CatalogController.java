package com.loginsight.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.loginsight.exception.InvalidQueryException;
import com.loginsight.service.CatalogService;

/**
 * Read-only metadata endpoints for the Algorithm Laboratory (docs/REBUILD_BASELINE Phase-2):
 * module descriptions with computed counts and the flat algorithm catalogue. The front-end uses
 * these to render the sidebar, the course map and the per-module labs.
 */
@RestController
@RequestMapping("/api")
public class CatalogController {

    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/modules")
    public ResponseEntity<?> modules() {
        return ResponseEntity.ok(catalog.modules());
    }

    @GetMapping("/modules/{id}")
    public ResponseEntity<?> module(@PathVariable String id) {
        return catalog.module(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new InvalidQueryException("Unknown module: " + id));
    }

    @GetMapping("/algorithms")
    public ResponseEntity<?> algorithms() {
        return ResponseEntity.ok(catalog.algorithms());
    }

    @GetMapping("/algorithms/{key}")
    public ResponseEntity<?> algorithm(@PathVariable String key) {
        return catalog.algorithm(key)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new InvalidQueryException("Unknown algorithm: " + key));
    }
}