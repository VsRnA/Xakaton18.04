package com.prodforge.backend.application.user;

public record CreateUserRequest(
        String phone,
        String password,
        String role,
        String username
) {}
