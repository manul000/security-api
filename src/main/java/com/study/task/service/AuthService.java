package com.study.task.service;

import com.study.task.dto.AuthResponse;
import com.study.task.dto.LoginRequest;
import com.study.task.dto.RegisterRequest;
import com.study.task.exception.ApiException;
import com.study.task.model.RefreshToken;
import com.study.task.model.Role;
import com.study.task.model.User;
import com.study.task.repository.RefreshTokenRepository;
import com.study.task.repository.UserRepository;
import com.study.task.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository users;
    private final RefreshTokenRepository refresh;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authentication;
    private final JwtService jwt;

    public AuthService(
            UserRepository users,
            RefreshTokenRepository refresh,
            PasswordEncoder encoder,
            AuthenticationManager authentication,
            JwtService jwt) {
        this.users = users;
        this.refresh = refresh;
        this.encoder = encoder;
        this.authentication = authentication;
        this.jwt = jwt;
    }

    @Transactional
    public AuthResponse register(RegisterRequest r) {
        String email = r.email().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(email))
            throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
        User user = users.save(new User(email, encoder.encode(r.password()), Role.ROLE_USER));
        return tokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest r) {
        String email = r.email().toLowerCase(Locale.ROOT);
        authentication.authenticate(new UsernamePasswordAuthenticationToken(email, r.password()));
        return tokens(users.findByEmail(email).orElseThrow());
    }

    private AuthResponse tokens(User user) {
        String access = jwt.generateToken(user);
        String token = UUID.randomUUID() + "-" + UUID.randomUUID();
        refresh.save(new RefreshToken(token, user, Instant.now().plusSeconds(7 * 24 * 3600)));
        return new AuthResponse(access, access, token);
    }

    @Transactional
    public AuthResponse refresh(String value) {
        RefreshToken old =
                refresh
                        .findByToken(value)
                        .orElseThrow(
                                () -> new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token not found"));
        if (!old.getExpiresAt().isAfter(Instant.now()))
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
        User user = old.getUser();
        refresh.delete(old);
        refresh.flush();
        return tokens(user);
    }

    @Transactional
    public void logout(User user) {
        refresh.deleteByUserId(user.getId());
    }
}
