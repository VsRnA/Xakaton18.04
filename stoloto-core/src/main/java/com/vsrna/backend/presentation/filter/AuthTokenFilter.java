package com.vsrna.backend.presentation.filter;

import com.vsrna.backend.infrastructure.security.JwtUtils;
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
            } catch (Exception ignored) {
                // невалидный токен — не устанавливаем атрибут, запрос продолжается без аутентификации
            }
        }

        chain.doFilter(request, response);
    }
}
