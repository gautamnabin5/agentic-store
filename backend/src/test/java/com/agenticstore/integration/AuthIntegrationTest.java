package com.agenticstore.integration;

import com.agenticstore.TestcontainersConfig;
import com.agenticstore.entity.User;
import com.agenticstore.entity.UserRole;
import com.agenticstore.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class AuthIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @AfterEach
    void cleanup() {
        userRepository.deleteAll();
    }

    @Test
    void register_withValidCredentials_returns201WithToken() throws Exception {
        var request = Map.of(
            "email", "newuser@example.com",
            "password", "password123",
            "name", "New User"
        );
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void register_createsUserInDatabase() throws Exception {
        var request = Map.of(
            "email", "dbcheck@example.com",
            "password", "password123",
            "name", "DB Check"
        );
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

        boolean exists = userRepository.findAll().stream()
            .anyMatch(u -> "dbcheck@example.com".equals(u.getEmail()));
        assertTrue(exists);
    }

    @Test
    void register_withDuplicateEmail_returns409() throws Exception {
        userRepository.save(User.builder()
            .email("existing@example.com")
            .passwordHash(passwordEncoder.encode("password123"))
            .name("Existing").role(UserRole.CUSTOMER).build());

        var request = Map.of(
            "email", "existing@example.com",
            "password", "password123",
            "name", "Duplicate"
        );
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict());
    }

    @Test
    void register_withPasswordTooShort_returns400() throws Exception {
        var request = Map.of(
            "email", "short@example.com",
            "password", "short",
            "name", "User"
        );
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void register_withBlankName_returns400() throws Exception {
        var request = Map.of(
            "email", "noname@example.com",
            "password", "password123",
            "name", ""
        );
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void login_withValidCredentials_returns200WithToken() throws Exception {
        userRepository.save(User.builder()
            .email("login@example.com")
            .passwordHash(passwordEncoder.encode("password123"))
            .name("Login User").role(UserRole.CUSTOMER).build());

        var request = Map.of("email", "login@example.com", "password", "password123");
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        userRepository.save(User.builder()
            .email("wrongpass@example.com")
            .passwordHash(passwordEncoder.encode("password123"))
            .name("User").role(UserRole.CUSTOMER).build());

        var request = Map.of("email", "wrongpass@example.com", "password", "incorrect!");
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void login_withUnknownEmail_returns401() throws Exception {
        var request = Map.of("email", "nobody@example.com", "password", "password123");
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/orders"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withInvalidToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/orders")
                .header("Authorization", "Bearer invalid_token"))
            .andExpect(status().isUnauthorized());
    }
}
