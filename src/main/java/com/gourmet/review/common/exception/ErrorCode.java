package com.gourmet.review.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "DUPLICATE_RESOURCE", "Resource already exists."),
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "ENTITY_NOT_FOUND", "Entity not found."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid credentials."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Invalid request."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication is required."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "Access is denied."),
    ADMIN_REQUIRED(HttpStatus.FORBIDDEN, "ADMIN_REQUIRED", "Admin privileges are required."),
    PHONE_VERIFICATION_REQUIRED(HttpStatus.FORBIDDEN, "PHONE_VERIFICATION_REQUIRED", "Phone verification is required."),
    OPERATION_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "OPERATION_NOT_ALLOWED", "Operation is not allowed."),
    FOLLOW_ALREADY_EXISTS(HttpStatus.CONFLICT, "FOLLOW_ALREADY_EXISTS", "Follow relation already exists."),
    FOLLOW_NOT_FOUND(HttpStatus.NOT_FOUND, "FOLLOW_NOT_FOUND", "Follow relation not found."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "Internal server error");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
