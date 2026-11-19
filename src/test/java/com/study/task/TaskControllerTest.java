package com.study.task;

import com.study.task.config.SecurityConfig;
import com.study.task.controller.TaskController;
import com.study.task.security.JwtFilter;
import com.study.task.security.JwtService;
import com.study.task.security.SecurityErrors;
import com.study.task.service.TaskService;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
@Import({SecurityConfig.class, JwtFilter.class, SecurityErrors.class})
class TaskControllerTest {
    @Autowired
    MockMvc mvc;
    @MockBean
    TaskService taskService;
    @MockBean
    JwtService jwtService;
    @MockBean
    UserDetailsService userDetailsService;

    @Test
    void getAll_whenNotAuthenticated_shouldReturn401() throws Exception {
        mvc.perform(get("/tasks"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @WithMockUser
    void getAll_whenAuthenticated_shouldReturn200() throws Exception {
        mvc.perform(get("/tasks")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void delete_whenRoleUser_shouldReturn403() throws Exception {
        mvc.perform(delete("/tasks/1")).andExpect(status().isForbidden());
        verifyNoInteractions(taskService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_whenRoleAdmin_shouldReturn204() throws Exception {
        mvc.perform(delete("/tasks/1")).andExpect(status().isNoContent());
        verify(taskService).delete(eq(1L), isNull());
    }

    @Test
    void expiredTokenShouldReturn401() throws Exception {
        when(jwtService.extractUsername("expired"))
                .thenThrow(new ExpiredJwtException(null, null, "expired"));
        mvc.perform(get("/tasks").header("Authorization", "Bearer expired"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Token expired"));
    }
}
