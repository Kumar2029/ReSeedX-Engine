package com.sba.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, List<Long>> requests = new ConcurrentHashMap<>();
    private static final int MAX_REQUESTS = 60;   // per minute
    private static final long WINDOW_MS   = 60_000;
    private static final int MAX_IPS      = 10_000;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        // Only rate-limit API endpoints
        if (!req.getRequestURI().startsWith("/api/")) {
            chain.doFilter(req, res); return;
        }

        String ip = req.getRemoteAddr();
        long now  = System.currentTimeMillis();

        // Evict stale IPs if map grows too large
        if (requests.size() > MAX_IPS) {
            requests.entrySet().removeIf(e -> e.getValue().stream().allMatch(t -> now - t > WINDOW_MS));
        }

        requests.compute(ip, (k, v) -> {
            if (v == null) v = new ArrayList<>();
            v.removeIf(t -> now - t > WINDOW_MS);
            v.add(now);
            return v;
        });

        if (requests.get(ip).size() > MAX_REQUESTS) {
            res.setStatus(429);
            res.setContentType("application/json");
            res.getWriter().write("{\"error\":\"Too many requests. Please wait 1 minute.\"}");
            return;
        }

        chain.doFilter(req, res);
    }
}
