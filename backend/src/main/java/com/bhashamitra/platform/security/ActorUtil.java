package com.bhashamitra.platform.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;

public final class ActorUtil {

    private ActorUtil() {}

    public static String actor(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) return "system";

        if (auth.getPrincipal() instanceof OAuth2User user) {
            String email = user.getAttribute("email");
            if (email != null && !email.isBlank()) return email;

            String username = user.getAttribute("cognito:username");
            if (username != null && !username.isBlank()) return username;
        }

        return auth.getName() != null ? auth.getName() : "system";
    }
}
