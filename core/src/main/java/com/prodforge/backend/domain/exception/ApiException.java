package com.prodforge.backend.domain.exception;

import lombok.Getter;

import java.util.UUID;

@Getter
public class ApiException extends RuntimeException {

    public enum ErrorCode {
        ERR_APP,
        ERR_CLIENT_BAD_REQUEST,
        ERR_CLIENT_REQUEST_VALIDATION,
        ERR_CLIENT_AUTH,
        ERR_CLIENT_ENTITY_ALREADY_EXIST,
        ERR_CLIENT_ENTITY_NOT_FOUND
    }

    private final String guid;
    private final ErrorCode code;
    private final int httpStatus;

    private ApiException(String message, ErrorCode code, int httpStatus) {
        super(message);
        this.guid = UUID.randomUUID().toString();
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public static ApiException internal(String message) {
        return new ApiException(message, ErrorCode.ERR_APP, 500);
    }

    public static ApiException badRequest(String message) {
        return new ApiException(message, ErrorCode.ERR_CLIENT_BAD_REQUEST, 400);
    }

    public static ApiException validationError(String message) {
        return new ApiException(message, ErrorCode.ERR_CLIENT_REQUEST_VALIDATION, 400);
    }

    public static ApiException unauthorized(String message) {
        return new ApiException(message, ErrorCode.ERR_CLIENT_AUTH, 401);
    }

    public static ApiException alreadyExists(String entity, String detail) {
        return new ApiException(
                String.format("Entity '%s' already exists. %s", entity, detail),
                ErrorCode.ERR_CLIENT_ENTITY_ALREADY_EXIST,
                409
        );
    }

    public static ApiException notFound(String entity, String detail) {
        return new ApiException(
                String.format("Entity '%s' not found. %s", entity, detail),
                ErrorCode.ERR_CLIENT_ENTITY_NOT_FOUND,
                404
        );
    }
}
