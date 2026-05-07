package com.agenticstore.service;

import com.agenticstore.common.Result;
import com.agenticstore.dto.auth.AuthResponse;
import com.agenticstore.dto.auth.LoginRequest;
import com.agenticstore.dto.auth.RegisterRequest;
import com.agenticstore.entity.User;
import com.agenticstore.entity.UserRole;
import com.agenticstore.repository.UserRepository;
import com.agenticstore.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public Result<AuthResponse> register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            return Result.failure("Email already in use", 409);
        }
        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setName(request.name());
        user.setRole(UserRole.CUSTOMER);

        User saved = userRepository.save(user);
        String token = jwtUtil.generateToken(saved.getId(), saved.getEmail(), saved.getRole().name());
        return Result.success(new AuthResponse(token));
    }

    public Result<AuthResponse> login(LoginRequest request) {
        return userRepository.findByEmail(request.email())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .map(u -> {
                    String token = jwtUtil.generateToken(u.getId(), u.getEmail(), u.getRole().name());
                    return Result.<AuthResponse>success(new AuthResponse(token));
                })
                .orElseGet(() -> Result.failure("Invalid credentials", 401));
    }
}
