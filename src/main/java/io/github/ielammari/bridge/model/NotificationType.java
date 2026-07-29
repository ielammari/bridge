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
	HIRED
}
