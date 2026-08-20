package io.github.ielammari.bridge.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/** An exam waiting on the expert it was handed to. */
public record PendingTechnicalDto(
		Integer applicationId,
		Integer candidateId,
		String candidateFirstName,
		String candidateLastName,
		Integer offerId,
		String offerTitle,
		LocalDate appointmentDate,
		LocalTime appointmentTime) {
}
