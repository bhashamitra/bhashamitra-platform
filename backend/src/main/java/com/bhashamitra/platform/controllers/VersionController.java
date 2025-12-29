package com.bhashamitra.platform.controllers;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VersionController {

    @GetMapping("/api/version")
    public Map<String, Object> version() {
        return Map.of(
                "name", "bhashamitra platform",
                "version", "1.0.0"
        );
    }
}

