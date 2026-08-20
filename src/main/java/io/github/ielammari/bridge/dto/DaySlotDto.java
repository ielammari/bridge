package io.github.ielammari.bridge.dto;

import io.github.ielammari.bridge.model.AppointmentType;

/** One hour of the scheduling grid, with what occupies it if taken. */
public record DaySlotDto(
		String time,
		boolean taken,
		Integer applicationId,
		String candidateName,
		String offerTitle,
		AppointmentType type) {
}
