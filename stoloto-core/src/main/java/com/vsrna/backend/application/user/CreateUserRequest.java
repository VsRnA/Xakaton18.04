package com.vsrna.backend.application.user;

public record CreateUserRequest(
        String phone,
        String password,
        String role,
        String username
) {}
