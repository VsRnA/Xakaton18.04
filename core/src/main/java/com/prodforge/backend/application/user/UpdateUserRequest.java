package com.prodforge.backend.application.user;

public record UpdateUserRequest(
        String username,
        String password,
        String name,
        String lastName,
        String patronymicName,
        String role
) {}
