package com.tooba.EduEvent.config;

import com.tooba.EduEvent.service.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                // Auth endpoints — fully public
                .requestMatchers("/api/auth/**").permitAll()

                // Static quiz HTML page — public
                .requestMatchers("/quiz.html").permitAll()

                // FIX 7: Each requestMatchers(HttpMethod, String) call takes ONE method + ONE path.
                // User-facing quiz session actions require authentication (JWT) but not ADMIN role.
                .requestMatchers(HttpMethod.POST, "/api/quiz/*/start").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/quiz/session/*/submit").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/quiz/session/*/violation").authenticated()

                // All other /api/quiz/** (create quiz, add questions, view results) → authenticated.
                // @PreAuthorize("hasRole('ADMIN')") on controller methods enforces the ADMIN check.
                .requestMatchers("/api/quiz/**").authenticated()

                // Event discovery — public
                .requestMatchers("/api/events", "/api/events/**").permitAll()

                // Certificate verification — public
                .requestMatchers("/api/certificates/verify/**").permitAll()

                // Uploaded files — public (static serving)
                .requestMatchers("/uploads/**").permitAll()

                // Everything else requires a valid JWT
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:8080"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Cache-Control"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}