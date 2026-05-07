package com.agenticstore.service;

import com.agenticstore.common.Result;
import com.agenticstore.dto.auth.AuthResponse;
import com.agenticstore.dto.auth.LoginRequest;
import com.agenticstore.dto.auth.RegisterRequest;

public interface AuthService {
    Result<AuthResponse> register(RegisterRequest request);
    Result<AuthResponse> login(LoginRequest request);
}
