package com.bhashamitra.platform.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class MeController {

    @GetMapping("/api/me")
    public ResponseEntity<Map<String, Object>> me(Authentication auth) {

        // If not authenticated (or not an OAuth2 user), return 200 OK with empty data
        // This prevents Spring Security from redirecting to OAuth2 login on public pages
        // The frontend will handle this gracefully by treating it as "not logged in"
        if (auth == null || !(auth.getPrincipal() instanceof OAuth2User user)) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("email", null);
            empty.put("username", null);
            empty.put("groups", List.of());
            return ResponseEntity.ok(empty);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("email", user.getAttribute("email"));
        out.put("username", user.getAttribute("cognito:username"));

        // Normalize groups to a JSON array consistently (your React expects string[])
        Object rawGroups = user.getAttribute("cognito:groups");
        if (rawGroups instanceof List<?> list) {
            out.put("groups", list.stream().map(String::valueOf).toList());
        } else if (rawGroups instanceof String s) {
            out.put("groups", List.of(s));
        } else {
            out.put("groups", List.of());
        }

        return ResponseEntity.ok(out);
    }
}
