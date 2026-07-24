package com.ata.rag.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Admin API scaffold. Full implementation lands in branch {@code be/admin-observability}.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @GetMapping("/summary")
    public void summary() {
        notImplemented("Admin summary");
    }

    @GetMapping("/failed-pages")
    public void failedPages() {
        notImplemented("Admin failed-pages");
    }

    @GetMapping("/questions")
    public void questions() {
        notImplemented("Admin questions");
    }

    @PostMapping("/sync")
    public void sync() {
        notImplemented("Admin website sync");
    }

    @PostMapping("/prices/sync")
    public void pricesSync() {
        notImplemented("Admin pricing sync");
    }

    private void notImplemented(String feature) {
        throw new ResponseStatusException(
                HttpStatus.NOT_IMPLEMENTED,
                feature + " is not implemented yet. See branch be/admin-observability and docs/openapi.yaml.");
    }
}
