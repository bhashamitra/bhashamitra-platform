package com.bhashamitra.platform.controllers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

    @GetMapping("/api/me")
    public ResponseEntity<Map<String, Object>> me(Authentication auth) {

        // If not authenticated (or not an OAuth2 user), return 401 instead of throwing NPE/ClassCastException
        if (auth == null || !(auth.getPrincipal() instanceof OAuth2User user)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
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
