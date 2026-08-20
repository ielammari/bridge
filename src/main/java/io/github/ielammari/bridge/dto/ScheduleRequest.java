package io.github.ielammari.bridge.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;

/**
 * HR picks the date and time of an interview from the hourly grid, and the
 * expert who runs it when it is a technical exam. An HR interview is run by the
 * recruiter who published the offer, so it carries no expert.
 */
public record ScheduleRequest(

		@NotNull(message = "La date est obligatoire.")
		LocalDate date,

		@NotNull(message = "L'heure est obligatoire.")
		LocalTime time,

		Integer expertId) {
}
