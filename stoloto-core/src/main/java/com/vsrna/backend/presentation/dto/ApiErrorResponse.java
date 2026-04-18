package com.vsrna.backend.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public final class ApiErrorResponse {

    private ApiErrorResponse() {}

    public static final String AUTH_ERROR_EXAMPLE = """
            {
              "error": {
                "guid": "61f0ed7c-4864-4bec-83c2-6e2d7eb97efd",
                "code": "ERR_CLIENT_AUTH",
                "message": "bearer token required"
              }
            }
            """;

    @Schema(description = "Детали ошибки")
    public record ErrorDetail(
            @Schema(description = "Уникальный идентификатор ошибки для трассировки",
                    example = "550e8400-e29b-41d4-a716-446655440000")
            String guid,

            @Schema(description = "Код ошибки",
                    example = "ERR_CLIENT_ENTITY_NOT_FOUND",
                    allowableValues = {
                            "ERR_APP",
                            "ERR_CLIENT_BAD_REQUEST",
                            "ERR_CLIENT_REQUEST_VALIDATION",
                            "ERR_CLIENT_AUTH",
                            "ERR_CLIENT_ENTITY_ALREADY_EXIST",
                            "ERR_CLIENT_ENTITY_NOT_FOUND"
                    })
            String code,

            @Schema(description = "Сообщение об ошибке",
                    example = "Entity 'User' not found. id=550e8400-e29b-41d4-a716-446655440000")
            String message
    ) {}

    @Schema(description = "Ответ с описанием ошибки")
    public record Body(
            @Schema(description = "Детали ошибки")
            ErrorDetail error
    ) {}
}
