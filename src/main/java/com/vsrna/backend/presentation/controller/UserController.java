package com.vsrna.backend.presentation.controller;

import com.vsrna.backend.application.user.UserService;
import com.vsrna.backend.domain.exception.ApiException;
import com.vsrna.backend.presentation.dto.ApiErrorResponse;
import com.vsrna.backend.presentation.dto.UserDto;
import com.vsrna.backend.presentation.filter.AuthTokenFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Управление пользователями")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Создать пользователя")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Пользователь создан"),
            @ApiResponse(responseCode = "400", description = "Некорректный запрос",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.Body.class))),
            @ApiResponse(responseCode = "401", description = "Не авторизован",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.Body.class),
                            examples = @ExampleObject(value = ApiErrorResponse.AUTH_ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "409", description = "Пользователь уже существует",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.Body.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto.UserResponse createUser(@Valid @RequestBody UserDto.CreateUserRequest request,
                                           HttpServletRequest httpRequest) {
        requireAuth(httpRequest);
        return UserDto.UserResponse.from(userService.createUser(request));
    }

    @Operation(summary = "Получить список пользователей")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список пользователей"),
            @ApiResponse(responseCode = "401", description = "Не авторизован",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.Body.class),
                            examples = @ExampleObject(value = ApiErrorResponse.AUTH_ERROR_EXAMPLE)))
    })
    @GetMapping
    public List<UserDto.UserResponse> listUsers(
            @Parameter(description = "Количество записей (1–100)", example = "20")
            @RequestParam(defaultValue = "20") int limit,
            @Parameter(description = "Смещение", example = "0")
            @RequestParam(defaultValue = "0") int offset,
            HttpServletRequest httpRequest) {
        requireAuth(httpRequest);
        int safeLimit = (limit <= 0 || limit > 100) ? 20 : limit;
        int safeOffset = Math.max(offset, 0);
        return userService.listUsers(safeLimit, safeOffset).stream()
                .map(UserDto.UserResponse::from)
                .toList();
    }

    @Operation(summary = "Получить пользователя по GUID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Пользователь найден"),
            @ApiResponse(responseCode = "401", description = "Не авторизован",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.Body.class),
                            examples = @ExampleObject(value = ApiErrorResponse.AUTH_ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.Body.class)))
    })
    @GetMapping("/{guid}")
    public UserDto.UserResponse getUser(@PathVariable UUID guid, HttpServletRequest httpRequest) {
        requireAuth(httpRequest);
        return UserDto.UserResponse.from(userService.getUser(guid));
    }

    @Operation(summary = "Обновить пользователя")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Пользователь обновлён"),
            @ApiResponse(responseCode = "400", description = "Некорректный запрос",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.Body.class))),
            @ApiResponse(responseCode = "401", description = "Не авторизован",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.Body.class),
                            examples = @ExampleObject(value = ApiErrorResponse.AUTH_ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.Body.class)))
    })
    @PutMapping("/{guid}")
    public UserDto.UserResponse updateUser(@PathVariable UUID guid,
                                           @Valid @RequestBody UserDto.UpdateUserRequest request,
                                           HttpServletRequest httpRequest) {
        requireAuth(httpRequest);
        return UserDto.UserResponse.from(userService.updateUser(guid, request));
    }

    @Operation(summary = "Удалить пользователя")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Пользователь удалён"),
            @ApiResponse(responseCode = "401", description = "Не авторизован",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.Body.class),
                            examples = @ExampleObject(value = ApiErrorResponse.AUTH_ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.Body.class)))
    })
    @DeleteMapping("/{guid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable UUID guid, HttpServletRequest httpRequest) {
        requireAuth(httpRequest);
        userService.deleteUser(guid);
    }

    private void requireAuth(HttpServletRequest request) {
        if (request.getAttribute(AuthTokenFilter.USER_ID_ATTR) == null) {
            throw ApiException.unauthorized("bearer token required");
        }
    }
}
