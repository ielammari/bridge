package io.github.ielammari.bridge.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * The context the expert needs to score an application: who, which offer,
 * which traits, and when the exam was booked for.
 */
public record TechnicalContextDto(
		Integer applicationId,
		Integer candidateId,
		String candidateFirstName,
		String candidateLastName,
		Integer offerId,
		String offerTitle,
		LocalDate appointmentDate,
		LocalTime appointmentTime,
		boolean waitForAppointment,
		List<ExaminedTraitDto> traits) {
}
