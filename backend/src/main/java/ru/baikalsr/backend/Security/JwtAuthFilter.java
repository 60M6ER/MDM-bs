package ru.baikalsr.backend.Security;

import jakarta.servlet.DispatcherType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtDecoder jwtDecoder;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(req, res);
            return;
        }

        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Jwt jwt = jwtDecoder.decode(token);
                String username = jwt.getSubject();

                // поддерживаем оба варианта: roles (List<String>) или scope (space-separated)
                java.util.List<String> roles = new java.util.ArrayList<>();
                Object rolesClaim = jwt.getClaim("roles");
                if (rolesClaim instanceof java.util.List<?> list) {
                    for (Object o : list) {
                        if (o != null) roles.add(o.toString());
                    }
                }
                if (roles.isEmpty()) {
                    String scope = (String) jwt.getClaim("scope");
                    if (scope != null) {
                        for (String s : scope.split("\\s+")) {
                            if (!s.isBlank()) roles.add(s);
                        }
                    }
                }

                if (roles.isEmpty()) {
                    // нет ролей — не аутентифицируем контекст, просто пропускаем дальше
                    chain.doFilter(req, res);
                    return;
                }

                var auths = roles.stream().map(SimpleGrantedAuthority::new).toList();
                var auth = new UsernamePasswordAuthenticationToken(username, null, auths);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtException e) {
                // недействительный токен — оставляем без аутентификации
            }
        }
        chain.doFilter(req, res);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String p = request.getServletPath();
        // исключаем все пути авторизации, например /api/v_1/auth/**
        return request.getDispatcherType() != DispatcherType.REQUEST
                || !p.startsWith("/api") || p.startsWith("/api") && p.contains("/auth/");
    }
}
