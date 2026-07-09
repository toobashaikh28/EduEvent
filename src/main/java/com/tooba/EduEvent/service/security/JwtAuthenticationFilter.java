package com.tooba.EduEvent.service.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tooba.EduEvent.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// FIX 5: Your filter already correctly sets "ROLE_" + role (e.g. "ROLE_ADMIN")
//        which makes @PreAuthorize("hasRole('ADMIN')") work — this was already correct.
//
//        Fixes added in this version:
//        - Expired/invalid token now returns a clean JSON 401 error
//          instead of letting the request fall through to a confusing auth error
//        - Added @Slf4j logging for security events
//        - Added null safety for edge cases

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    // Guards against ever re-processing an internally forwarded/error dispatch
    // of the same request — the root cause of the StackOverflowError we saw in
    // production, where a failed response write triggered a forward that
    // re-entered this filter, which failed to write again, forwarded again,
    // and so on until the stack overflowed.
    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return true;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Belt-and-suspenders: if this exact request somehow re-enters this
        // filter (e.g. via an internal forward), skip straight through instead
        // of re-running auth logic and risking another failed write + loop.
        final String guardAttr = "EduEvent.jwtFilterRan";
        if (request.getAttribute(guardAttr) != null) {
            filterChain.doFilter(request, response);
            return;
        }
        request.setAttribute(guardAttr, Boolean.TRUE);

        try {
            final String authHeader = request.getHeader("Authorization");

            // No Authorization header — pass through (public routes handled by SecurityConfig)
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            String jwt = authHeader.substring(7);

            // If token is expired or malformed, return clean JSON 401 instead of crashing
            String userEmail;
            try {
                userEmail = jwtUtil.extractEmail(jwt);
            } catch (Exception ex) {
                log.warn("Invalid or malformed JWT token: {}", ex.getMessage());
                sendUnauthorizedError(response, "Invalid or expired token. Please log in again.");
                return;
            }

            // Only set authentication if not already authenticated in this request
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Validate token before trusting it
                if (!jwtUtil.isTokenValid(jwt, userEmail)) {
                    log.warn("JWT token is no longer valid for user: {}", userEmail);
                    sendUnauthorizedError(response, "Token expired. Please log in again.");
                    return;
                }

                // Load role from DB and set "ROLE_ADMIN" / "ROLE_USER" / "ROLE_JUDGE"
                // This is what makes @PreAuthorize("hasRole('ADMIN')") work correctly
                // hasRole('ADMIN') checks for authority "ROLE_ADMIN" — Spring adds ROLE_ prefix automatically
                List<SimpleGrantedAuthority> authorities = userRepository
                        .findByEmail(userEmail)
                        .map(u -> List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole())))
                        .orElse(List.of());

                if (authorities.isEmpty()) {
                    log.warn("JWT token references a user that no longer exists: {}", userEmail);
                    sendUnauthorizedError(response, "User account not found.");
                    return;
                }

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(userEmail, null, authorities);
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.debug("Authenticated user: {} with authorities: {}", userEmail, authorities);
            }

            filterChain.doFilter(request, response);

        } catch (Exception ex) {
            // Never let an unexpected failure here cascade into a forward/retry
            // loop — log it and, only if nothing has been written yet, return a
            // clean 500 instead of letting the container's own error handling
            // re-dispatch back through the filter chain.
            log.error("Unexpected error in JwtAuthenticationFilter", ex);
            if (!response.isCommitted()) {
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            }
        }
    }

    // Writes a clean JSON 401 error response.
    // Guarded against writing to an already-committed response, which is what
    // was silently failing and triggering the forward/recursion loop before.
    private void sendUnauthorizedError(HttpServletResponse response, String message)
            throws IOException {
        if (response.isCommitted()) {
            log.warn("Tried to send 401 ({}) but response was already committed — skipping.", message);
            return;
        }
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("timestamp", LocalDateTime.now().toString());
        errorBody.put("status", 401);
        errorBody.put("error", "Unauthorized");
        errorBody.put("message", message);

        response.getWriter().write(objectMapper.writeValueAsString(errorBody));
    }
}