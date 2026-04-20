package com.vsrna.game.domain.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.UUID;

@Getter
public class ApiException extends RuntimeException {

    public enum ErrorCode {
        ERR_APP,
        ERR_CLIENT_BAD_REQUEST,
        ERR_CLIENT_REQUEST_VALIDATION,
        ERR_CLIENT_AUTH,
        ERR_CLIENT_ENTITY_ALREADY_EXIST,
        ERR_CLIENT_ENTITY_NOT_FOUND,
        ERR_CLIENT_INSUFFICIENT_BALANCE
    }

    private final String guid;
    private final ErrorCode code;
    private final HttpStatus status;
    private final Map<String, Object> details;

    private ApiException(String message, ErrorCode code, HttpStatus status) {
        super(message);
        this.guid = UUID.randomUUID().toString();
        this.code = code;
        this.status = status;
        this.details = null;
    }

    private ApiException(String message, ErrorCode code, HttpStatus status, Map<String, Object> details) {
        super(message);
        this.guid = UUID.randomUUID().toString();
        this.code = code;
        this.status = status;
        this.details = details;
    }

    public static ApiException internal(String message) {
        return new ApiException(message, ErrorCode.ERR_APP, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public static ApiException badRequest(String message) {
        return new ApiException(message, ErrorCode.ERR_CLIENT_BAD_REQUEST, HttpStatus.BAD_REQUEST);
    }

    public static ApiException validationError(String message) {
        return new ApiException(message, ErrorCode.ERR_CLIENT_REQUEST_VALIDATION, HttpStatus.BAD_REQUEST);
    }

    public static ApiException unauthorized(String message) {
        return new ApiException(message, ErrorCode.ERR_CLIENT_AUTH, HttpStatus.UNAUTHORIZED);
    }

    public static ApiException alreadyExists(String entity, String detail) {
        return new ApiException(
                String.format("Entity '%s' already exists. %s", entity, detail),
                ErrorCode.ERR_CLIENT_ENTITY_ALREADY_EXIST,
                HttpStatus.CONFLICT
        );
    }

    public static ApiException notFound(String entity, String detail) {
        return new ApiException(
                String.format("Entity '%s' not found. %s", entity, detail),
                ErrorCode.ERR_CLIENT_ENTITY_NOT_FOUND,
                HttpStatus.NOT_FOUND
        );
    }

    public static ApiException insufficientBalance(String message, Map<String, Object> details) {
        return new ApiException(message, ErrorCode.ERR_CLIENT_INSUFFICIENT_BALANCE,
                HttpStatus.PAYMENT_REQUIRED, details);
    }
}
