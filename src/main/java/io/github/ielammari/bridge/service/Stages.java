package io.github.ielammari.bridge.service;

import io.github.ielammari.bridge.model.AppointmentType;
import io.github.ielammari.bridge.model.ApplicationStatus;

/** Maps an application's stage to the interview it is waiting on, if any. */
final class Stages {

	private Stages() {
	}

	static AppointmentType appointmentTypeFor(ApplicationStatus status) {
		return switch (status) {
			case EXAMEN_TECHNIQUE -> AppointmentType.TECHNIQUE;
			case ENTRETIEN_RH -> AppointmentType.RH;
			default -> null;
		};
	}

}
