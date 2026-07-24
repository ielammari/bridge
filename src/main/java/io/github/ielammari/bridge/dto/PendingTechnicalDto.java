package io.github.ielammari.bridge.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/** An application awaiting the technical exam, as the expert sees it. */
public record PendingTechnicalDto(
		Integer applicationId,
		String candidateFirstName,
		String candidateLastName,
		String offerTitle,
		LocalDate appointmentDate,
		LocalTime appointmentTime) {
}
