package com.agenticstore.controller;

import com.agenticstore.common.Result;
import com.agenticstore.dto.auth.AuthResponse;
import com.agenticstore.dto.auth.LoginRequest;
import com.agenticstore.dto.auth.RegisterRequest;
import com.agenticstore.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public Result<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public Result<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
