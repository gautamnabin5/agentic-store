package com.agenticstore.service;

import com.agenticstore.common.Result;
import com.agenticstore.dto.auth.AuthResponse;
import com.agenticstore.dto.auth.LoginRequest;
import com.agenticstore.dto.auth.RegisterRequest;
import com.agenticstore.entity.User;
import com.agenticstore.entity.UserRole;
import com.agenticstore.repository.UserRepository;
import com.agenticstore.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    UserRepository userRepository = Mockito.mock(UserRepository.class);
    PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
    JwtUtil jwtUtil;
    AuthService authService;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(
            "test-secret-key-that-is-long-enough-for-hmac-sha256-algorithm",
            86400000L
        );
        authService = new AuthServiceImpl(userRepository, passwordEncoder, jwtUtil);
    }

    @Test
    void register_withNewEmail_returnsSuccessWithToken() {
        UUID userId = UUID.randomUUID();
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        User saved = User.builder()
                .id(userId).email("new@example.com")
                .name("Alice").role(UserRole.CUSTOMER).build();
        when(userRepository.save(any())).thenReturn(saved);

        Result<AuthResponse> result = authService.register(
                new RegisterRequest("new@example.com", "password123", "Alice"));

        assertInstanceOf(Result.Success.class, result);
        assertNotNull(((Result.Success<AuthResponse>) result).value().token());
        assertTrue(((Result.Success<AuthResponse>) result).value().token().length() > 0);
    }

    @Test
    void register_withExistingEmail_returnsFailure409() {
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        Result<AuthResponse> result = authService.register(
                new RegisterRequest("taken@example.com", "password123", "Alice"));

        assertInstanceOf(Result.Failure.class, result);
        assertEquals(409, ((Result.Failure<AuthResponse>) result).httpStatus());
    }

    @Test
    void login_withValidCredentials_returnsSuccessWithToken() {
        User user = User.builder()
                .id(UUID.randomUUID()).email("user@example.com")
                .passwordHash("hashed").name("Alice").role(UserRole.CUSTOMER).build();
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);

        Result<AuthResponse> result = authService.login(
                new LoginRequest("user@example.com", "password123"));

        assertInstanceOf(Result.Success.class, result);
        assertNotNull(((Result.Success<AuthResponse>) result).value().token());
    }

    @Test
    void login_withWrongPassword_returnsFailure401() {
        User user = User.builder()
                .id(UUID.randomUUID()).email("user@example.com")
                .passwordHash("hashed").name("Alice").role(UserRole.CUSTOMER).build();
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        Result<AuthResponse> result = authService.login(
                new LoginRequest("user@example.com", "wrong"));

        assertInstanceOf(Result.Failure.class, result);
        assertEquals(401, ((Result.Failure<AuthResponse>) result).httpStatus());
    }

    @Test
    void login_withUnknownEmail_returnsFailure401() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        Result<AuthResponse> result = authService.login(
                new LoginRequest("nobody@example.com", "password123"));

        assertInstanceOf(Result.Failure.class, result);
        assertEquals(401, ((Result.Failure<AuthResponse>) result).httpStatus());
    }
}
