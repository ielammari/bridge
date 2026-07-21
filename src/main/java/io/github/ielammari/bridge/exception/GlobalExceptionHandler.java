package io.github.ielammari.bridge.exception;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns every failure into the single {@link ErrorResponse} shape.
 * <p>
 * Extends {@code ResponseEntityExceptionHandler} so Spring's own web
 * exceptions (unknown route, bad method, unreadable body) keep their status
 * codes instead of collapsing into 500.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
			MethodArgumentNotValidException exception,
			HttpHeaders headers,
			HttpStatusCode status,
			WebRequest request) {

		String details = exception.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + " : " + error.getDefaultMessage())
				.collect(Collectors.joining(", "));

		return handleExceptionInternal(exception,
				new ErrorResponse("VALIDATION_FAILED", details),
				headers, HttpStatus.BAD_REQUEST, request);
	}

	/**
	 * Gives Spring's built in exceptions the same body shape as ours, without
	 * changing the status code Spring already chose.
	 */
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(
			Exception exception,
			Object body,
			HttpHeaders headers,
			HttpStatusCode statusCode,
			WebRequest request) {

		Object payload = (body instanceof ErrorResponse)
				? body
				: new ErrorResponse(codeFor(statusCode), messageFor(statusCode));

		return super.handleExceptionInternal(exception, payload, headers, statusCode, request);
	}

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ErrorResponse> handleApi(ApiException exception) {
		return ResponseEntity.status(exception.getStatus())
				.body(new ErrorResponse(exception.getCode(), exception.getMessage()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
		log.error("Unhandled exception", exception);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ErrorResponse("INTERNAL_ERROR",
						"Une erreur interne est survenue. Veuillez réessayer plus tard."));
	}

	private String codeFor(HttpStatusCode statusCode) {
		if (statusCode.value() == HttpStatus.NOT_FOUND.value()) {
			return "RESOURCE_NOT_FOUND";
		}
		if (statusCode.value() == HttpStatus.METHOD_NOT_ALLOWED.value()) {
			return "METHOD_NOT_ALLOWED";
		}
		return statusCode.is4xxClientError() ? "BAD_REQUEST" : "INTERNAL_ERROR";
	}

	private String messageFor(HttpStatusCode statusCode) {
		if (statusCode.value() == HttpStatus.NOT_FOUND.value()) {
			return "La ressource demandée est introuvable.";
		}
		if (statusCode.value() == HttpStatus.METHOD_NOT_ALLOWED.value()) {
			return "Cette méthode n'est pas autorisée pour cette ressource.";
		}
		return statusCode.is4xxClientError()
				? "La requête est invalide."
				: "Une erreur interne est survenue. Veuillez réessayer plus tard.";
	}

}
