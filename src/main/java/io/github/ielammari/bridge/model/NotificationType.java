package io.github.ielammari.bridge.model;

/**
 * Category of a system notification, used by the inbox for tone and grouping.
 * Stored in the optional type_notification column.
 */
public enum NotificationType {
	APPLICATION_RECEIVED,
	SCHEDULE_NEEDED,
	INTERVIEW_SCHEDULED,
	REJECTED,
	HIRED;

	/**
	 * Whether a notification of this kind ever reaches an account of this role.
	 * A preference is offered only for what the account actually receives, so
	 * this decides the settings page's list as well as delivery.
	 */
	public boolean isSentTo(Role role) {
		return switch (this) {
			case APPLICATION_RECEIVED, SCHEDULE_NEEDED -> role == Role.RH;
			case INTERVIEW_SCHEDULED -> role == Role.CANDIDAT || role == Role.EXPERT;
			case REJECTED, HIRED -> role == Role.CANDIDAT;
		};
	}

	/**
	 * Whether a user of this role may turn this notification off.
	 *
	 * Silenceability depends on who is reading, not on the kind alone: the same
	 * scheduled interview is a work queue to the expert, who may mute it, and
	 * the candidate's own appointment, which they may not. A refusal and a
	 * hiring are the decision itself rather than a prompt about it, so they are
	 * always delivered.
	 */
	public boolean isSilenceableBy(Role role) {
		if (!isSentTo(role)) {
			return false;
		}
		return switch (this) {
			case APPLICATION_RECEIVED, SCHEDULE_NEEDED -> true;
			case INTERVIEW_SCHEDULED -> role == Role.EXPERT;
			case REJECTED, HIRED -> false;
		};
	}
}
