package io.github.ielammari.bridge.dto;

/**
 * A technical expert an exam can be handed to. The load is how many interviews
 * they already hold that week, so the work can be spread rather than guessed.
 */
public record ExpertSummaryDto(
		Integer id,
		String firstName,
		String lastName,
		String email,
		int load) {
}
