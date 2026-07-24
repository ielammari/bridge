package io.github.ielammari.bridge.model;

/** Status of an appointment. Names match the CHECK constraint on rendez_vous.statut. */
public enum AppointmentStatus {
	PLANIFIE,
	REALISE,
	ANNULE
}
