package io.github.ielammari.bridge.model;

/** Which set of interviews a calendar request asks for. */
public enum CalendarScope {
	/** The interviews the caller runs themselves. */
	MINE,
	/** The exams a recruiter arranged on the offers they published. */
	PLANNED,
	/** One named evaluator's own calendar, opened by a recruiter who is booking. */
	EVALUATOR
}
