package com.agenticstore.controller;

import com.agenticstore.common.Result;
import com.agenticstore.dto.auth.AuthResponse;
import com.agenticstore.dto.auth.LoginRequest;
import com.agenticstore.dto.auth.RegisterRequest;
import com.agenticstore.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        return switch (authService.register(request)) {
            case Result.Success<AuthResponse> s -> ResponseEntity.status(201).body(s.value());
            case Result.Failure<AuthResponse> f -> ResponseEntity.status(f.httpStatus()).body(Map.of("error", f.error()));
        };
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        return switch (authService.login(request)) {
            case Result.Success<AuthResponse> s -> ResponseEntity.ok(s.value());
            case Result.Failure<AuthResponse> f -> ResponseEntity.status(f.httpStatus()).body(Map.of("error", f.error()));
        };
    }
}
