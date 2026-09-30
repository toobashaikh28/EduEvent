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
                // 1. Public Assets: Allow all frontend files in root and subfolders
                // (role dashboards are static shells — their DATA endpoints below
                //  still require JWT, and each page also JS-guards by role)
                .requestMatchers("/", "/*.html", "/css/**", "/js/**", "/images/**", "/pages/**",
                                 "/user/**", "/admin/**", "/judge/**", "/favicon.ico").permitAll()

                // Swagger / OpenAPI docs — publicly viewable API reference
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**",
                                 "/v3/api-docs.yaml").permitAll()

                // 2. Auth endpoints: Fully public
                .requestMatchers("/api/auth/**", "/api/health").permitAll()

                // 3. Public API resources
                // FIX: was permitAll() for ALL HTTP methods on /api/events/**, which
                // made POST/DELETE /api/events/{id}/register reachable anonymously
                // (feature list says: "All endpoints protected by JWT except login,
                // register, PUBLIC EVENTS, certificate verify" — public BROWSING only).
                // Only GET (browsing/filtering events) is public now. Registering,
                // cancelling, and admin create/update/delete all require a valid JWT.
                .requestMatchers(HttpMethod.GET, "/api/events", "/api/events/**").permitAll()
                .requestMatchers("/api/certificates/verify/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/hackathon/*/results").permitAll() // public rankings (feature list)
                .requestMatchers("/uploads/**").permitAll()

                // 4. Protected Quiz endpoints (Authenticated users only)
                .requestMatchers(HttpMethod.POST, "/api/quiz/*/start").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/quiz/session/*/submit").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/quiz/session/*/violation").authenticated()

                // 5. Default: Everything else requires a valid JWT
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
        configuration.setAllowedOrigins(List.of("http://localhost:8080"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Cache-Control"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
