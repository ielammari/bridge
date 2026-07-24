package io.github.ielammari.bridge.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;

/** HR picks the date and time of an interview from the hourly grid. */
public record ScheduleRequest(

		@NotNull(message = "La date est obligatoire.")
		LocalDate date,

		@NotNull(message = "L'heure est obligatoire.")
		LocalTime time) {
}
