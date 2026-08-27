package io.github.ielammari.bridge.dto;

import java.util.List;

/**
 * Where a candidate stands against one offer. An offer is compatible when the
 * degree is met and no required trait is missing; plus traits never appear
 * here, since they gate nothing.
 */
public record MatchDto(
		boolean compatible,
		boolean degreeMet,
		List<String> missingTraits) {
}
