package com.agenticstore.security;

import java.util.UUID;

public record UserPrincipal(UUID id, String email, String role) {}
