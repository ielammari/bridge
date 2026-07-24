package io.github.ielammari.bridge.model;

/**
 * Outcome of an evaluation. Names match the CHECK constraint on
 * evaluation.decision. The sequence diagrams narrate these as favorable and
 * unfavorable, but the stored values are VALIDEE and REFUSEE.
 */
public enum Decision {
	VALIDEE,
	REFUSEE
}
