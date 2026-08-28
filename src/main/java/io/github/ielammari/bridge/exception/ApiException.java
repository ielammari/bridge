package io.github.ielammari.bridge.exception;

import org.springframework.http.HttpStatus;

/**
 * A failure the client is expected to handle: carries the status, the English
 * code application code branches on, and the French message a user reads.
 */
public final class ApiException extends RuntimeException {

	private final HttpStatus status;
	private final String code;

	public ApiException(HttpStatus status, String code, String message) {
		super(message);
		this.status = status;
		this.code = code;
	}

	public static ApiException emailAlreadyUsed() {
		return new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_USED",
				"Un compte existe déjà avec cette adresse email.");
	}

	public static ApiException invalidCredentials() {
		return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
				"L'adresse email ou le mot de passe est incorrect.");
	}

	public static ApiException conflict(String code, String message) {
		return new ApiException(HttpStatus.CONFLICT, code, message);
	}

	public static ApiException unauthorized(String code, String message) {
		return new ApiException(HttpStatus.UNAUTHORIZED, code, message);
	}

	public static ApiException notFound(String message) {
		return new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", message);
	}

	public static ApiException forbidden(String code, String message) {
		return new ApiException(HttpStatus.FORBIDDEN, code, message);
	}

	public static ApiException badRequest(String code, String message) {
		return new ApiException(HttpStatus.BAD_REQUEST, code, message);
	}

	public static ApiException internal(String code, String message) {
		return new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, code, message);
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getCode() {
		return code;
	}

}
