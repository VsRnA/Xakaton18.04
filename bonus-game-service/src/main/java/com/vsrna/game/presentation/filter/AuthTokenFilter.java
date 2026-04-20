package com.vsrna.game.presentation.filter;

import com.vsrna.game.infrastructure.security.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuthTokenFilter extends OncePerRequestFilter {

    public static final String USER_ID_ATTR = "userId";
    public static final String USERNAME_ATTR = "username";

    private final JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String rawToken = header.substring(7);
            try {
                Claims claims = jwtUtils.validateToken(rawToken);
                UUID userId = UUID.fromString(claims.getSubject());
                request.setAttribute(USER_ID_ATTR, userId);
                request.setAttribute(USERNAME_ATTR, claims.get("username", String.class));
            } catch (Exception ignored) {
            }
        }

        chain.doFilter(request, response);
    }
}
