package io.github.ielammari.bridge.model;

/**
 * Category of a system notification, used by the inbox for tone and grouping.
 * Stored in the optional type_notification column.
 */
public enum NotificationType {
	APPLICATION_RECEIVED,
	APPLICATION_SUBMITTED,
	SCHEDULE_NEEDED,
	INTERVIEW_SCHEDULED,
	EXAM_UNASSIGNED,
	EXAM_OVERDUE,
	REJECTED,
	HIRED;

	/**
	 * Whether a notification of this kind reaches an account of this role. A
	 * preference is offered only for what the account receives, so this decides
	 * the settings list as well as delivery.
	 */
	public boolean isSentTo(Role role) {
		return switch (this) {
			case APPLICATION_RECEIVED, SCHEDULE_NEEDED, EXAM_OVERDUE -> role == Role.RH;
			case INTERVIEW_SCHEDULED -> role == Role.CANDIDAT || role == Role.EXPERT;
			case EXAM_UNASSIGNED -> role == Role.EXPERT;
			case APPLICATION_SUBMITTED, REJECTED, HIRED -> role == Role.CANDIDAT;
		};
	}

	/**
	 * Whether a user of this role may turn this notification off. What can be
	 * silenced is what only reports on the funnel: an interview names who must
	 * be there, and a refusal or a hiring is the decision itself, so those are
	 * always delivered.
	 */
	public boolean isSilenceableBy(Role role) {
		if (!isSentTo(role)) {
			return false;
		}
		return switch (this) {
			case APPLICATION_RECEIVED, APPLICATION_SUBMITTED, SCHEDULE_NEEDED, EXAM_OVERDUE -> true;
			case INTERVIEW_SCHEDULED, EXAM_UNASSIGNED, REJECTED, HIRED -> false;
		};
	}
}
