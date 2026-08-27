package io.github.ielammari.bridge.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import io.github.ielammari.bridge.model.ApplicationStatus;

/** An application as HR sees it in the list for one offer. */
public record HrApplicationDto(
		Integer id,
		Integer candidateId,
		String candidateFirstName,
		String candidateLastName,
		String candidateEmail,
		Integer offerId,
		String offerTitle,
		Instant applicationDate,
		ApplicationStatus status,
		LocalDate appointmentDate,
		LocalTime appointmentTime,
		Integer appointmentEvaluatorId,
		String appointmentEvaluatorName,
		boolean waitForAppointment) {
}
