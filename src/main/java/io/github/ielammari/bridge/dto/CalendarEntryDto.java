package io.github.ielammari.bridge.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import io.github.ielammari.bridge.model.ApplicationStatus;
import io.github.ielammari.bridge.model.AppointmentType;

/**
 * One interview on a calendar, carrying enough of its application to name it
 * and to open it. `recorded` says its result is already in.
 */
public record CalendarEntryDto(
		Integer applicationId,
		Integer candidateId,
		String candidateFirstName,
		String candidateLastName,
		Integer offerId,
		String offerTitle,
		LocalDate date,
		LocalTime time,
		AppointmentType type,
		ApplicationStatus applicationStatus,
		Integer evaluatorId,
		String evaluatorName,
		boolean recorded) {
}
