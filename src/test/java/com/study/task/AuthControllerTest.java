package com.study.task;

import com.study.task.config.SecurityConfig;
import com.study.task.controller.AuthController;
import com.study.task.dto.AuthResponse;
import com.study.task.exception.ApiException;
import com.study.task.security.JwtFilter;
import com.study.task.security.JwtService;
import com.study.task.security.SecurityErrors;
import com.study.task.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtFilter.class, SecurityErrors.class})
class AuthControllerTest {
    @Autowired
    MockMvc mvc;
    @MockBean
    AuthService authService;
    @MockBean
    JwtService jwtService;
    @MockBean
    UserDetailsService userDetailsService;
    String valid = "{\"email\":\"alice@example.com\",\"password\":\"password123\"}";

    @Test
    void registerValid() throws Exception {
        when(authService.register(any())).thenReturn(new AuthResponse("access", "access", "refresh"));
        mvc.perform(post("/auth/register").contentType("application/json").content(valid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("access"));
    }

    @Test
    void duplicateEmail409() throws Exception {
        when(authService.register(any()))
                .thenThrow(new ApiException(HttpStatus.CONFLICT, "Email already registered"));
        mvc.perform(post("/auth/register").contentType("application/json").content(valid))
                .andExpect(status().isConflict());
    }

    @Test
    void shortPassword400() throws Exception {
        mvc.perform(
                        post("/auth/register")
                                .contentType("application/json")
                                .content("{\"email\":\"alice@example.com\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(authService);
    }

    @Test
    void loginValid() throws Exception {
        when(authService.login(any())).thenReturn(new AuthResponse("access", "access", "refresh"));
        mvc.perform(post("/auth/login").contentType("application/json").content(valid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("access"));
    }

    @Test
    void loginWrongPassword401() throws Exception {
        when(authService.login(any())).thenThrow(new BadCredentialsException("wrong"));
        mvc.perform(post("/auth/login").contentType("application/json").content(valid))
                .andExpect(status().isUnauthorized());
    }
}
