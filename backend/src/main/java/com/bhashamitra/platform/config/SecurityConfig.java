package com.bhashamitra.platform.config;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.http.HttpStatus;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            CognitoLogoutSuccessHandler logoutSuccessHandler) throws Exception {
        http
                // CSRF off for APIs; also ignore /logout so logout works without CSRF token plumbing
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/logout"))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/index.html", "/assets/**", "/favicon.ico").permitAll()
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/api/admin/**").hasAnyRole("admin", "editor")
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
                )

                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(oidcUserServiceWithCognitoGroups())
                        )
                        // After successful login, redirect to home page
                        .defaultSuccessUrl("/", true)
                )

                // Configure exception handling to prevent auto-redirects to OAuth2 login
                // CRITICAL: Spring Security with OAuth2 login will automatically redirect
                // unauthenticated users to OAuth2 provider. We need to prevent this for:
                // 1. API requests - return 401 instead of redirect
                // 2. Public routes - allow through without authentication
                .exceptionHandling(exceptions -> exceptions
                        // For API requests, return 401 instead of redirecting to OAuth login
                        // This MUST be configured BEFORE the default entry point to take precedence
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                (request) -> {
                                    String uri = request.getRequestURI();
                                    return uri.startsWith("/api/");
                                }
                        )
                        // For all other requests (including /, /index.html, etc.),
                        // they are permitAll() so they should NOT trigger authentication
                        // Return OK to allow the request through without any redirect
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.OK),
                                (request) -> {
                                    String uri = request.getRequestURI();
                                    return !uri.startsWith("/api/") && !uri.startsWith("/oauth2/");
                                }
                        )
                )

                .logout(logout -> logout
                        .logoutUrl("/logout")          // default, but explicit is clearer
                        .logoutSuccessHandler(logoutSuccessHandler)
                );

        return http.build();
    }

    /**
     * Map Cognito groups from the OIDC user attributes into Spring Security roles.
     * Cognito provides groups under the "cognito:groups" claim, e.g. ["admin"].
     * Spring role checks like hasAnyRole("admin") require authorities like "ROLE_admin".
     */
    @Bean
    OAuth2UserService<OidcUserRequest, OidcUser> oidcUserServiceWithCognitoGroups() {
        OidcUserService delegate = new OidcUserService();

        return (userRequest) -> {
            OidcUser oidcUser = delegate.loadUser(userRequest);

            Set<GrantedAuthority> mappedAuthorities = new HashSet<>(oidcUser.getAuthorities());

            Object groupsObj = oidcUser.getAttributes().get("cognito:groups");
            if (groupsObj instanceof Collection<?> groups) {
                for (Object g : groups) {
                    if (g == null) continue;
                    String group = String.valueOf(g).trim();
                    if (!group.isEmpty()) {
                        // Keep group names as-is (admin/editor) to match hasAnyRole("admin","editor")
                        mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_" + group));
                    }
                }
            }

            // Recreate user with mapped authorities.
            // Use "sub" as the name attribute key (stable unique subject).
            return new DefaultOidcUser(
                    mappedAuthorities,
                    oidcUser.getIdToken(),
                    oidcUser.getUserInfo(),
                    "sub"
            );
        };
    }
}
