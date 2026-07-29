package io.github.ielammari.bridge.dto;

import java.time.Instant;

import io.github.ielammari.bridge.model.NotificationType;

/** A message in the recipient's inbox. */
public record MessageDto(
		Integer id,
		String content,
		Instant sentAt,
		boolean read,
		NotificationType type,
		Integer applicationId) {
}
