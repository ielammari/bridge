package io.github.ielammari.bridge.dto;

import java.time.Instant;

public record AuthResponse(
		String token,
		Instant expiresAt,
		UserSummary user) {
}
