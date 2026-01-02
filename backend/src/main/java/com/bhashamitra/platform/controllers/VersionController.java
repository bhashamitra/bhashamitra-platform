package com.bhashamitra.platform.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class VersionController {

    @Value("${app.version:0.0.0-dev}")
    private String appVersion;

    @Value("${app.buildSha:unknown}")
    private String buildSha;

    @Value("${app.buildTime:unknown}")
    private String buildTime;

    @GetMapping("/api/public/version")
    public Map<String, Object> version() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("name", "bhashamitra platform");
        out.put("version", appVersion);
        out.put("build", buildSha);
        out.put("builtAt", buildTime);
        return out;
    }
}

