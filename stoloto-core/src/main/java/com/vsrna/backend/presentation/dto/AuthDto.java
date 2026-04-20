package com.vsrna.backend.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDto {

    private AuthDto() {}

    public record LoginRequest(
            @NotBlank String phone,
            @NotBlank String password
    ) {}

    public record RegisterRequest(
            @NotBlank @Pattern(regexp = "^\\+?[0-9]{7,15}$") String phone,
            @NotBlank @Size(min = 8, max = 72) String password
    ) {}

    public record LoginResponse(
            String token,
            UserDto.UserResponse user
    ) {}
}
