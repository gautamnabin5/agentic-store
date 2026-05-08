package com.agenticstore.integration;

import com.agenticstore.TestcontainersConfig;
import com.agenticstore.entity.User;
import com.agenticstore.entity.UserRole;
import com.agenticstore.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfig.class)
class AuthIntegrationTest {

    @Autowired TestRestTemplate restTemplate;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @AfterEach
    void cleanup() {
        userRepository.deleteAll();
    }

    @Test
    void register_withValidCredentials_returns201WithToken() {
        var request = Map.of(
            "email", "newuser@example.com",
            "password", "password123",
            "name", "New User"
        );
        ResponseEntity<Map> resp = restTemplate.postForEntity("/api/v1/auth/register", request, Map.class);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertNotNull(resp.getBody().get("token"));
    }

    @Test
    void register_createsUserInDatabase() {
        var request = Map.of(
            "email", "dbcheck@example.com",
            "password", "password123",
            "name", "DB Check"
        );
        restTemplate.postForEntity("/api/v1/auth/register", request, Map.class);

        boolean exists = userRepository.findAll().stream()
            .anyMatch(u -> "dbcheck@example.com".equals(u.getEmail()));
        assertTrue(exists);
    }

    @Test
    void register_withDuplicateEmail_returns409() {
        userRepository.save(User.builder()
            .email("existing@example.com")
            .passwordHash(passwordEncoder.encode("password123"))
            .name("Existing").role(UserRole.CUSTOMER).build());

        var request = Map.of(
            "email", "existing@example.com",
            "password", "password123",
            "name", "Duplicate"
        );
        ResponseEntity<Map> resp = restTemplate.postForEntity("/api/v1/auth/register", request, Map.class);

        assertEquals(HttpStatus.CONFLICT, resp.getStatusCode());
    }

    @Test
    void register_withPasswordTooShort_returns400() {
        var request = Map.of(
            "email", "short@example.com",
            "password", "short",
            "name", "User"
        );
        ResponseEntity<Map> resp = restTemplate.postForEntity("/api/v1/auth/register", request, Map.class);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void register_withBlankName_returns400() {
        var request = Map.of(
            "email", "noname@example.com",
            "password", "password123",
            "name", ""
        );
        ResponseEntity<Map> resp = restTemplate.postForEntity("/api/v1/auth/register", request, Map.class);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void login_withValidCredentials_returns200WithToken() {
        userRepository.save(User.builder()
            .email("login@example.com")
            .passwordHash(passwordEncoder.encode("password123"))
            .name("Login User").role(UserRole.CUSTOMER).build());

        var request = Map.of("email", "login@example.com", "password", "password123");
        ResponseEntity<Map> resp = restTemplate.postForEntity("/api/v1/auth/login", request, Map.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertNotNull(resp.getBody().get("token"));
    }

    @Test
    void login_withWrongPassword_returns401() {
        userRepository.save(User.builder()
            .email("wrongpass@example.com")
            .passwordHash(passwordEncoder.encode("password123"))
            .name("User").role(UserRole.CUSTOMER).build());

        var request = Map.of("email", "wrongpass@example.com", "password", "incorrect!");
        ResponseEntity<Map> resp = restTemplate.postForEntity("/api/v1/auth/login", request, Map.class);

        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void login_withUnknownEmail_returns401() {
        var request = Map.of("email", "nobody@example.com", "password", "password123");
        ResponseEntity<Map> resp = restTemplate.postForEntity("/api/v1/auth/login", request, Map.class);

        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void protectedEndpoint_withoutToken_returns401() {
        ResponseEntity<Map> resp = restTemplate.getForEntity("/api/v1/orders", Map.class);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void protectedEndpoint_withInvalidToken_returns401() {
        restTemplate.getRestTemplate().getInterceptors().clear();
        ResponseEntity<Map> resp = restTemplate.getForEntity(
            "/api/v1/orders",
            Map.class,
            Map.of()
        );
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }
}
