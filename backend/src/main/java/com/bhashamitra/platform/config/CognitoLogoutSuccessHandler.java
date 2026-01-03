package com.bhashamitra.platform.config;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class CognitoLogoutSuccessHandler implements LogoutSuccessHandler {

    private final String cognitoDomain;
    private final String cognitoClientId;

    public CognitoLogoutSuccessHandler(
            @Value("${cognito.domain}") String cognitoDomain,
            @Value("${COGNITO_CLIENT_ID}") String cognitoClientId
    ) {
        this.cognitoDomain = stripTrailingSlash(cognitoDomain);
        this.cognitoClientId = cognitoClientId;
    }

    @Override
    public void onLogoutSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        // Build return URL based on the current request (honors ALB forwarded headers because you set forward-headers-strategy=framework)
        String baseUrl = request.getScheme() + "://" + request.getServerName();
        int port = request.getServerPort();

        boolean isDefaultPort = ("http".equals(request.getScheme()) && port == 80)
                || ("https".equals(request.getScheme()) && port == 443);

        if (!isDefaultPort) {
            baseUrl += ":" + port;
        }

        String logoutUri = baseUrl + "/";

        String redirect =
                cognitoDomain + "/logout"
                        + "?client_id=" + url(cognitoClientId)
                        + "&logout_uri=" + url(logoutUri);

        response.setStatus(HttpServletResponse.SC_FOUND);
        response.sendRedirect(redirect);
    }

    private static String url(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String stripTrailingSlash(String s) {
        return s != null && s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }
}
