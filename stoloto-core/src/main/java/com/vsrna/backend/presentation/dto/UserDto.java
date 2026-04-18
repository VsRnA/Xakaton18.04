package com.vsrna.backend.presentation.dto;

import com.vsrna.backend.domain.user.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class UserDto {

    private UserDto() {}

    public record CreateUserRequest(
            @NotBlank @Pattern(regexp = "^\\+?[0-9]{7,15}$") String phone,
            @NotBlank @Size(min = 8, max = 72) String password,
            @Pattern(regexp = "^(admin|user)$") String role
    ) {}

    public record UpdateUserRequest(
            @Size(min = 3, max = 100) String username,
            @Size(min = 8, max = 72) String password,
            @Size(max = 100) String name,
            @Size(max = 100) String lastName,
            @Size(max = 100) String patronymicName
    ) {}

    public record UserResponse(
            UUID guid,
            String phone,
            String username,
            String name,
            String lastName,
            String patronymicName,
            List<String> roles,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static UserResponse from(User user) {
            return new UserResponse(
                    user.getGuid(),
                    user.getPhone(),
                    user.getUsername(),
                    user.getName(),
                    user.getLastName(),
                    user.getPatronymicName(),
                    List.copyOf(user.getRoleKeywords()),
                    user.getCreatedAt(),
                    user.getUpdatedAt()
            );
        }
    }
}
