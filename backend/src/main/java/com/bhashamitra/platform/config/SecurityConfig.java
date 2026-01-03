package com.bhashamitra.platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

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

                .oauth2Login(Customizer.withDefaults())

                .logout(logout -> logout
                        .logoutUrl("/logout")          // default, but explicit is clearer
                        .logoutSuccessHandler(logoutSuccessHandler)
                );

        return http.build();
    }
}
