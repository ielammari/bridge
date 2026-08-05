package io.github.ielammari.bridge.model;

import java.io.Serializable;
import java.util.Objects;

/** Composite key of a silenced notification: who, and which type. */
public class NotificationPreferenceId implements Serializable {

	private Integer userId;
	private NotificationType type;

	public NotificationPreferenceId() {
	}

	public NotificationPreferenceId(Integer userId, NotificationType type) {
		this.userId = userId;
		this.type = type;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof NotificationPreferenceId key)) {
			return false;
		}
		return Objects.equals(userId, key.userId) && type == key.type;
	}

	@Override
	public int hashCode() {
		return Objects.hash(userId, type);
	}

}
