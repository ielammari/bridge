package io.github.ielammari.bridge.model;

/**
 * Lifecycle of an application. Names match the CHECK constraint on
 * candidature.statut. Rejection and hiring are terminal.
 */
public enum ApplicationStatus {
	NOUVELLE,
	EN_REVUE,
	EXAMEN_TECHNIQUE,
	ENTRETIEN_RH,
	REFUSEE,
	EMBAUCHEE
}
