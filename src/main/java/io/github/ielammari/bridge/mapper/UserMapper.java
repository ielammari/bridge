package io.github.ielammari.bridge.mapper;

import io.github.ielammari.bridge.dto.UserSummary;
import io.github.ielammari.bridge.model.User;

/**
 * Maps a user onto the shape returned at the API boundary. A pure
 * transformation with no dependencies, so it is a utility rather than a bean.
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
				user.getRole(),
				user.isMustChangePassword(),
				user.mustCompleteProfile(),
				user.getGoogleSub() != null,
				user.getPasswordHash() != null);
	}

}
