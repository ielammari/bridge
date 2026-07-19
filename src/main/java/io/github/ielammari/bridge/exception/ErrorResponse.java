package io.github.ielammari.bridge.exception;

/**
 * The single error shape returned by every endpoint: a machine readable code
 * and a human readable message. The frontend API client reads exactly this.
 */
public record ErrorResponse(String code, String message) {
}
