package com.study.task;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.task.model.RefreshToken;
import com.study.task.model.Role;
import com.study.task.model.User;
import com.study.task.repository.RefreshTokenRepository;
import com.study.task.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthFlowIntegrationTest {
    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper mapper;
    @Autowired
    RefreshTokenRepository refresh;
    @Autowired
    UserRepository users;
    @Autowired
    org.springframework.security.crypto.password.PasswordEncoder encoder;

    JsonNode register(String email) throws Exception {
        return mapper.readTree(
                mvc.perform(
                                post("/auth/register")
                                        .contentType("application/json")
                                        .content(
                                                mapper.writeValueAsString(
                                                        new com.study.task.dto.RegisterRequest(email, "password123"))))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());
    }

    @Test
    void registerLoginAndRead() throws Exception {
        register("login@test.com");
        var result =
                mvc.perform(
                                post("/auth/login")
                                        .contentType("application/json")
                                        .content("{\"email\":\"login@test.com\",\"password\":\"password123\"}"))
                        .andExpect(status().isOk())
                        .andReturn();
        String token =
                mapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
        mvc.perform(get("/tasks").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        assertThat(users.findByEmail("login@test.com").orElseThrow().getPassword()).startsWith("$2");
    }

    @Test
    void userCannotDeleteAndDuplicate409() throws Exception {
        var a = register("duplicate@test.com");
        mvc.perform(delete("/tasks/1").header("Authorization", "Bearer " + a.get("token").asText()))
                .andExpect(status().isForbidden());
        mvc.perform(
                        post("/auth/register")
                                .contentType("application/json")
                                .content("{\"email\":\"duplicate@test.com\",\"password\":\"password123\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void ownershipAcrossUsers() throws Exception {
        String a = register("alice@test.com").get("token").asText(),
                b = register("bob@test.com").get("token").asText();
        var created =
                mvc.perform(
                                post("/tasks")
                                        .header("Authorization", "Bearer " + a)
                                        .contentType("application/json")
                                        .content("{\"title\":\"Alice task\",\"priority\":\"HIGH\"}"))
                        .andExpect(status().isCreated())
                        .andReturn();
        long id = mapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(get("/tasks/my").header("Authorization", "Bearer " + a))
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/tasks/my").header("Authorization", "Bearer " + b))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/tasks/" + id).header("Authorization", "Bearer " + b))
                .andExpect(status().isForbidden());
        mvc.perform(
                        put("/tasks/" + id)
                                .header("Authorization", "Bearer " + b)
                                .contentType("application/json")
                                .content("{\"title\":\"Other task\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/tasks/" + id + "/own").header("Authorization", "Bearer " + b))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/tasks/" + id + "/own").header("Authorization", "Bearer " + a))
                .andExpect(status().isNoContent());
    }

    @Test
    void refreshRotatesAndLogoutRevokes() throws Exception {
        var a = register("refresh@test.com");
        String old = a.get("refreshToken").asText();
        var result =
                mvc.perform(
                                post("/auth/refresh")
                                        .contentType("application/json")
                                        .content(mapper.writeValueAsString(new com.study.task.dto.RefreshRequest(old))))
                        .andExpect(status().isOk())
                        .andReturn();
        var rotated = mapper.readTree(result.getResponse().getContentAsString());
        mvc.perform(
                        post("/auth/refresh")
                                .contentType("application/json")
                                .content(mapper.writeValueAsString(new com.study.task.dto.RefreshRequest(old))))
                .andExpect(status().isUnauthorized());
        mvc.perform(
                        post("/auth/logout").header("Authorization", "Bearer " + rotated.get("token").asText()))
                .andExpect(status().isNoContent());
        mvc.perform(
                        post("/auth/refresh")
                                .contentType("application/json")
                                .content(
                                        mapper.writeValueAsString(
                                                new com.study.task.dto.RefreshRequest(
                                                        rotated.get("refreshToken").asText()))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredRefresh401() throws Exception {
        register("expired@test.com");
        refresh.saveAndFlush(
                new RefreshToken(
                        "expired",
                        users.findByEmail("expired@test.com").orElseThrow(),
                        Instant.now().minusSeconds(1)));
        mvc.perform(
                        post("/auth/refresh")
                                .contentType("application/json")
                                .content("{\"refreshToken\":\"expired\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminDeletesTask() throws Exception {
        String user = register("owner@test.com").get("token").asText();
        users.saveAndFlush(new User("admin@test.com", encoder.encode("password123"), Role.ROLE_ADMIN));
        var logged =
                mvc.perform(
                                post("/auth/login")
                                        .contentType("application/json")
                                        .content("{\"email\":\"admin@test.com\",\"password\":\"password123\"}"))
                        .andExpect(status().isOk())
                        .andReturn();
        String admin = mapper.readTree(logged.getResponse().getContentAsString()).get("token").asText();
        var created =
                mvc.perform(
                                post("/tasks")
                                        .header("Authorization", "Bearer " + user)
                                        .contentType("application/json")
                                        .content("{\"title\":\"Delete me\"}"))
                        .andExpect(status().isCreated())
                        .andReturn();
        long id = mapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(delete("/tasks/" + id).header("Authorization", "Bearer " + admin))
                .andExpect(status().isNoContent());
    }

    @Test
    void malformedTokenAndDeletedUserReturn401() throws Exception {
        mvc.perform(get("/tasks").header("Authorization", "Bearer broken"))
                .andExpect(status().isUnauthorized());
        String token = register("removed@test.com").get("token").asText();
        var user = users.findByEmail("removed@test.com").orElseThrow();
        refresh.deleteByUserId(user.getId());
        refresh.flush();
        users.delete(user);
        users.flush();
        mvc.perform(get("/tasks").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registrationCannotGrantAdminRole() throws Exception {
        mvc.perform(
                        post("/auth/register")
                                .contentType("application/json")
                                .content(
                                        "{\"email\":\"role@test.com\",\"password\":\"password123\",\"role\":\"ROLE_ADMIN\"}"))
                .andExpect(status().isOk());
        assertThat(users.findByEmail("role@test.com").orElseThrow().getRole())
                .isEqualTo(Role.ROLE_USER);
    }
}
