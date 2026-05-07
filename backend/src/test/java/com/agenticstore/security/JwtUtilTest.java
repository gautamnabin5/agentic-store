package com.agenticstore.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(
            "test-secret-key-that-is-long-enough-for-hmac-sha256-algorithm",
            86400000L
        );
    }

    @Test
    void generateAndParse_roundTrip() {
        String token = jwtUtil.generateToken(userId, "user@example.com", "CUSTOMER");
        var claims = jwtUtil.parseToken(token);
        assertEquals(userId.toString(), claims.getSubject());
        assertEquals("user@example.com", claims.get("email", String.class));
        assertEquals("CUSTOMER", claims.get("role", String.class));
    }

    @Test
    void isTokenValid_withValidToken_returnsTrue() {
        String token = jwtUtil.generateToken(userId, "user@example.com", "CUSTOMER");
        assertTrue(jwtUtil.isTokenValid(token));
    }

    @Test
    void isTokenValid_withTamperedToken_returnsFalse() {
        assertFalse(jwtUtil.isTokenValid("not.a.real.token"));
    }
}
