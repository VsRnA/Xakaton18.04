package com.prodforge.backend.presentation.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class InternalAuthFilter extends OncePerRequestFilter {

    private final String internalSecret;

    public InternalAuthFilter(@Value("${app.internal.secret}") String internalSecret) {
        this.internalSecret = internalSecret;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String secret = request.getHeader("X-Internal-Secret");
        if (!internalSecret.equals(secret)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid internal secret");
            return;
        }
        chain.doFilter(request, response);
    }
}
