package com.agenticstore.service;

import com.agenticstore.common.Result;
import com.agenticstore.dto.auth.AuthResponse;
import com.agenticstore.dto.auth.LoginRequest;
import com.agenticstore.dto.auth.RegisterRequest;
import com.agenticstore.entity.User;
import com.agenticstore.entity.UserRole;
import com.agenticstore.repository.UserRepository;
import com.agenticstore.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;

    private JwtUtil jwtUtil;
    private AuthService authService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        // Create a real JwtUtil instance with test values
        jwtUtil = new JwtUtil(
            "test-secret-key-that-is-long-enough-for-hmac-sha256-algorithm",
            86400000L
        );
        authService = new AuthService(userRepository, passwordEncoder, jwtUtil);
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
