package io.github.ielammari.bridge.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import io.github.ielammari.bridge.model.AppointmentStatus;
import io.github.ielammari.bridge.model.AppointmentType;

/** An interview that was booked, past or upcoming, and who runs it. */
public record AppointmentDto(
		Integer id,
		AppointmentType type,
		AppointmentStatus status,
		LocalDate date,
		LocalTime time,
		String evaluatorName) {
}
