package com.vsrna.backend.presentation.controller;

import com.vsrna.backend.application.auth.AuthService;
import com.vsrna.backend.presentation.dto.ApiErrorResponse;
import com.vsrna.backend.presentation.dto.AuthDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Аутентификация пользователей")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Зарегистрироваться", description = "Создаёт нового пользователя и возвращает JWT-токен")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Пользователь зарегистрирован"),
            @ApiResponse(responseCode = "400", description = "Некорректный запрос",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.Body.class))),
            @ApiResponse(responseCode = "409", description = "Пользователь уже существует",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.Body.class)))
    })
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthDto.LoginResponse register(@Valid @RequestBody AuthDto.RegisterRequest request) {
        return authService.register(request);
    }

    @Operation(summary = "Войти в систему", description = "Возвращает JWT-токен для авторизации")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешная авторизация"),
            @ApiResponse(responseCode = "400", description = "Некорректный запрос",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.Body.class))),
            @ApiResponse(responseCode = "401", description = "Неверный телефон или пароль",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.Body.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "error": {
                                        "guid": "61f0ed7c-4864-4bec-83c2-6e2d7eb97efd",
                                        "code": "ERR_CLIENT_AUTH",
                                        "message": "invalid phone or password"
                                      }
                                    }
                                    """)))
    })
    @PostMapping("/login")
    public AuthDto.LoginResponse login(@Valid @RequestBody AuthDto.LoginRequest request) {
        return authService.login(request);
    }
}
