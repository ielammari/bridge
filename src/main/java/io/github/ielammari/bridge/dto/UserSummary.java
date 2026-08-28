package io.github.ielammari.bridge.dto;

import io.github.ielammari.bridge.model.Role;

/** Identity returned to the client. Never exposes the password hash. */
public record UserSummary(
		Integer id,
		String email,
		String firstName,
		String lastName,
		Role role,
		boolean mustChangePassword,
		boolean mustCompleteProfile,
		boolean googleLinked,
		boolean hasPassword) {
}
