package com.bhashamitra.platform.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class VersionController {

    @GetMapping("/api/public/version")
    public Map<String, Object> version() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("name", "bhashamitra platform");
        out.put("version", env("APP_VERSION", "0.0.0-dev"));
        out.put("build", env("APP_BUILD_SHA", "unknown"));
        out.put("builtAt", env("APP_BUILD_TIME", "unknown"));
        return out;
    }

    private static String env(String key, String defaultValue) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? defaultValue : v;
    }
}
