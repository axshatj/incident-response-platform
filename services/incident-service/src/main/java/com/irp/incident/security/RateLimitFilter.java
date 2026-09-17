package com.irp.incident.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * In-memory sliding window. PostgreSQL remains source of truth; this is only
 * abuse protection for the demo API. Replace with Redis/gateway limits in prod.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private final ObjectMapper mapper;
    private final int maxPerMinute;
    private final ConcurrentHashMap<String, Deque<Long>> windows = new ConcurrentHashMap<>();

    public RateLimitFilter(ObjectMapper mapper, int maxPerMinute) {
        this.mapper = mapper;
        this.maxPerMinute = maxPerMinute;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        String key = clientKey(request);
        long now = System.currentTimeMillis();
        long cutoff = now - 60_000L;
        Deque<Long> hits = windows.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (hits) {
            while (!hits.isEmpty() && hits.peekFirst() < cutoff) {
                hits.removeFirst();
            }
            if (hits.size() >= maxPerMinute) {
                response.setStatus(429);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("timestamp", Instant.now().toString());
                body.put("status", 429);
                body.put("message", "Rate limit exceeded");
                mapper.writeValue(response.getOutputStream(), body);
                return;
            }
            hits.addLast(now);
        }
        filterChain.doFilter(request, response);
    }

    private static String clientKey(HttpServletRequest request) {
        String actor = request.getHeader(AuthFilter.ACTOR_HEADER);
        String ip = request.getRemoteAddr();
        return (actor == null || actor.isBlank() ? ip : actor) + "|" + ip;
    }
}
