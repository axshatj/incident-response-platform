package com.irp.incident.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Header RBAC for local/demo: {@code X-IRP-Role} and {@code X-IRP-Actor}.
 * Swap for OIDC at the gateway before any shared deployment.
 */
public class AuthFilter extends OncePerRequestFilter {

    public static final String ROLE_HEADER = "X-IRP-Role";
    public static final String ACTOR_HEADER = "X-IRP-Actor";

    private final ObjectMapper mapper;

    public AuthFilter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        Role role = Role.parse(request.getHeader(ROLE_HEADER));
        if (role == null) {
            write(response, HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid " + ROLE_HEADER);
            return;
        }
        if (!AccessPolicy.allows(role, request.getMethod(), request.getRequestURI())) {
            write(response, HttpServletResponse.SC_FORBIDDEN,
                    "Role " + role + " cannot " + request.getMethod() + " " + request.getRequestURI());
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void write(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status);
        body.put("message", message);
        mapper.writeValue(response.getOutputStream(), body);
    }
}
