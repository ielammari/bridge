package io.github.ielammari.bridge.dto;

/** One qualification on the candidate's academic path, as read. */
public record EducationDto(
		Integer id,
		String title,
		String institution,
		String fieldOfStudy,
		Short startYear,
		Short endYear) {
}
