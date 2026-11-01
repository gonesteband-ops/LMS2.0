package com.lms.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final int MAX_LOGIN_ATTEMPTS = 30;
    private static final int MAX_API_ATTEMPTS = 200;
    private static final long WINDOW_MS = 60000; // 1 minuto

    private final Map<String, RateLimitEntry> attempts = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Determinar lÃ­mite segÃºn el endpoint
        int maxAttempts;
        if (path.equals("/api/v1/auth/login") || path.equals("/login")) {
            maxAttempts = MAX_LOGIN_ATTEMPTS;
        } else if (path.startsWith("/api/v1/")) {
            maxAttempts = MAX_API_ATTEMPTS;
        } else {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = getClientIP(request);
        RateLimitEntry entry = attempts.compute(ip, (k, v) -> {
            if (v == null || System.currentTimeMillis() - v.timestamp > WINDOW_MS) {
                return new RateLimitEntry(1);
            }
            v.count.incrementAndGet();
            return v;
        });

        if (entry.count.get() > maxAttempts) {
            log.warn("[RATE-LIMIT] IP {} bloqueada por exceder {} intentos/min en {}", ip, maxAttempts, path);
            response.setStatus(429);
            String hxRequest = request.getHeader("HX-Request");
            if ("true".equals(hxRequest)) {
                response.setContentType("text/html;charset=UTF-8");
                response.getWriter().write("<div style='padding:var(--spacing-md);background:#fef2f2;border:1px solid #fecaca;border-radius:var(--border-radius-lg);font-size:var(--font-size-sm);color:#991b1b;' role='alert'>Demasiadas solicitudes. Espera 1 minuto antes de intentar de nuevo.</div>");
            } else {
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Demasiadas solicitudes. Intenta en 1 minuto.\"}");
            }
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIP(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        return (xf != null && !xf.isEmpty()) ? xf.split(",")[0].trim() : request.getRemoteAddr();
    }

    private static class RateLimitEntry {
        final AtomicInteger count;
        final long timestamp;

        RateLimitEntry(int initial) {
            this.count = new AtomicInteger(initial);
            this.timestamp = System.currentTimeMillis();
        }
    }
}

