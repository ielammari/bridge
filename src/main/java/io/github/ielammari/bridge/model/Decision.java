package io.github.ielammari.bridge.model;

/**
 * Outcome of an evaluation. The names match the CHECK constraint on
 * evaluation.decision, so the constraint and the enum cannot drift.
 */
public enum Decision {
	VALIDEE,
	REFUSEE
}
