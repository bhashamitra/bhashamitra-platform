package com.bhashamitra.platform.controllers;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

    @GetMapping("/api/me")
    public Map<String, Object> me(Authentication auth) {
        OAuth2User user = (OAuth2User) auth.getPrincipal();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("email", user.getAttribute("email"));
        out.put("username", user.getAttribute("cognito:username"));
        out.put("groups", user.getAttribute("cognito:groups")); // may be null
        return out;
    }
}
