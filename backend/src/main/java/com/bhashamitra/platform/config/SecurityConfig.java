package com.bhashamitra.platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF off for APIs (ok for now)
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))

                .authorizeHttpRequests(auth -> auth
                        // React + static
                        .requestMatchers(
                                "/", "/index.html", "/assets/**", "/favicon.ico"
                        ).permitAll()

                        // Actuator
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()

                        // APIs require login
                        .requestMatchers("/api/**").authenticated()

                        // SPA routes
                        .anyRequest().permitAll()
                )

                // This is what enables Cognito login
                .oauth2Login(Customizer.withDefaults())

                .logout(Customizer.withDefaults());

        return http.build();
    }
}
