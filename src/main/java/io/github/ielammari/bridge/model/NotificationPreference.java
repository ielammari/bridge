package io.github.ielammari.bridge.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * One notification a user has chosen not to receive. A row exists only for a
 * silenced type, so the default is always delivery.
 */
@Entity
@Table(name = "preference_notification")
@IdClass(NotificationPreferenceId.class)
public class NotificationPreference {

	@Id
	@Column(name = "id_utilisateur")
	private Integer userId;

	@Id
	@Enumerated(EnumType.STRING)
	@Column(name = "type_notification", length = 30)
	private NotificationType type;

	protected NotificationPreference() {
	}

	public NotificationPreference(Integer userId, NotificationType type) {
		this.userId = userId;
		this.type = type;
	}

	public Integer getUserId() {
		return userId;
	}

	public NotificationType getType() {
		return type;
	}

}
