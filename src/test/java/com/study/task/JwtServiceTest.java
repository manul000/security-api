package com.study.task;

import com.study.task.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {
    JwtService jwt;
    org.springframework.security.core.userdetails.UserDetails user =
            User.withUsername("alice@example.com").password("unused").roles("USER").build();

    @BeforeEach
    void setup() {
        jwt = new JwtService();
        ReflectionTestUtils.setField(jwt, "secret", "dGVzdFNlY3JldEtleUZvclVuaXRUZXN0c09ubHkzMkJ5dGVz");
        ReflectionTestUtils.setField(jwt, "expiration", 900000L);
    }

    @Test
    void generateToken_shouldContainUsername() {
        String token = jwt.generateToken(user);
        assertThat(jwt.extractUsername(token)).isEqualTo(user.getUsername());
    }

    @Test
    void isTokenValid_withValidToken_shouldReturnTrue() {
        assertThat(jwt.isTokenValid(jwt.generateToken(user), user)).isTrue();
    }

    @Test
    void extractUsername_shouldReturnCorrectEmail() {
        assertThat(jwt.extractUsername(jwt.generateToken(user))).isEqualTo("alice@example.com");
    }

    @Test
    void expiredTokenIsInvalid() {
        Instant now = Instant.now();
        ReflectionTestUtils.setField(jwt, "clock", Clock.fixed(now, ZoneOffset.UTC));
        String token = jwt.generateToken(user);
        ReflectionTestUtils.setField(jwt, "clock", Clock.fixed(now.plusSeconds(3600), ZoneOffset.UTC));
        assertThat(jwt.isTokenValid(token, user)).isFalse();
    }

    @Test
    void wrongUserAndTamperingRejected() {
        String token = jwt.generateToken(user);
        assertThat(
                jwt.isTokenValid(
                        token, User.withUsername("other@example.com").password("x").roles("USER").build()))
                .isFalse();
        assertThat(jwt.isTokenValid(token.substring(0, token.length() - 10) + "invalid", user))
                .isFalse();
    }
}
