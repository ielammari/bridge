package io.github.ielammari.bridge.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import io.github.ielammari.bridge.exception.ApiException;
import io.github.ielammari.bridge.model.Appointment;
import io.github.ielammari.bridge.model.JobOffer;

/**
 * When an interview may be recorded. An offer that holds its evaluators to the
 * schedule refuses one before the booked hour; an offer that waives the rule
 * accepts it as soon as the interview is planned.
 */
final class Timing {

	private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH'h'mm");

	private Timing() {
	}

	/**
	 * Whether the booked hour has come, or the offer does not ask for it. The
	 * hour itself counts. Pure, so it is testable.
	 */
	static boolean isOpen(boolean waits, LocalDate date, LocalTime time, LocalDateTime now) {
		return !waits || !LocalDateTime.of(date, time).isAfter(now);
	}

	static void requireOpen(JobOffer offer, Appointment appointment) {
		if (isOpen(offer.isWaitForAppointment(), appointment.getDate(), appointment.getTime(),
				LocalDateTime.now())) {
			return;
		}
		throw ApiException.badRequest("INTERVIEW_NOT_DUE",
				"Cet entretien est prévu le " + DAY.format(appointment.getDate())
						+ " à " + HOUR.format(appointment.getTime())
						+ ". Il ne peut être évalué qu'une fois son heure venue.");
	}

}
