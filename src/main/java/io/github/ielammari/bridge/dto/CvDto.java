package io.github.ielammari.bridge.dto;

import java.time.Instant;

/** One CV on file, as offered when applying. `isDefault` is the one proposed. */
public record CvDto(
		Integer id,
		String label,
		Instant uploadedAt,
		boolean isDefault) {
}
