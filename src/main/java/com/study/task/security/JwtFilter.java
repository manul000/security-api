package com.study.task.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final UserDetailsService users;
    private final SecurityErrors errors;

    public JwtFilter(JwtService jwt, UserDetailsService users, SecurityErrors errors) {
        this.jwt = jwt;
        this.users = users;
        this.errors = errors;
    }

    protected void doFilterInternal(
            HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(req, res);
            return;
        }
        try {
            String token = header.substring(7);
            String email = jwt.extractUsername(token);
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails user = users.loadUserByUsername(email);
                if (!jwt.isTokenValid(token, user)) {
                    errors.write(res, 401, "Invalid token");
                    return;
                }
                SecurityContextHolder.getContext()
                        .setAuthentication(
                                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
            }
        } catch (ExpiredJwtException e) {
            SecurityContextHolder.clearContext();
            errors.write(res, 401, "Token expired");
            return;
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
            SecurityContextHolder.clearContext();
            errors.write(res, 401, "Invalid token");
            return;
        }
        chain.doFilter(req, res);
    }
}
