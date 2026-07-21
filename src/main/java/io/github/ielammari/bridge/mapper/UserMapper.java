package io.github.ielammari.bridge.mapper;

import io.github.ielammari.bridge.dto.UserSummary;
import io.github.ielammari.bridge.model.User;

/**
 * Maps a user onto the shape returned at the API boundary.
 * <p>
 * A pure transformation with no dependencies, so it is a final utility rather
 * than an injected bean: there is nothing here worth substituting in a test.
 */
public final class UserMapper {

	private UserMapper() {
	}

	public static UserSummary toSummary(User user) {
		return new UserSummary(
				user.getId(),
				user.getEmail(),
				user.getFirstName(),
				user.getLastName(),
				user.getRole());
	}

}
