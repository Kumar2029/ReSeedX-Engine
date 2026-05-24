package com.sba.config;

import com.sba.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String path = req.getRequestURI();
        // Public paths - no JWT needed
        if ((path.startsWith("/api/auth/") && !path.equals("/api/auth/me")) || path.startsWith("/swagger-ui") ||
            path.startsWith("/v3/api-docs") || path.endsWith(".html") ||
            path.endsWith(".css") || path.endsWith(".js") || path.endsWith(".png") ||
            path.equals("/") || path.startsWith("/static")) {
            chain.doFilter(req, res); return;
        }
        String token = null;
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) token = header.substring(7);
        // Fallback: query param ?token= (used for PDF downloads)
        if (token == null || token.equals("null")) token = req.getParameter("token");

        if (token != null && !token.equals("null") && jwtUtil.isValid(token)) {
            req.setAttribute("clientId", jwtUtil.extractClientId(token));
            req.setAttribute("username", jwtUtil.extractUsername(token));
            req.setAttribute("role", jwtUtil.extractRole(token));
            chain.doFilter(req, res);
        } else if (path.startsWith("/api/")) {
            res.setStatus(401);
            res.setContentType("application/json");
            res.getWriter().write("{\"error\":\"Unauthorized - valid JWT required\"}");
        } else {
            chain.doFilter(req, res);
        }
    }
}
