package io.github.ielammari.bridge.dto;

import java.time.LocalDate;
import java.util.List;

import io.github.ielammari.bridge.model.Degree;
import io.github.ielammari.bridge.model.Gender;

/**
 * Everything one actor is allowed to know about a candidate, gathered at the
 * candidate's own address rather than scattered across the applications they
 * appear in.
 *
 * The reader is decided before this is built, so what reaches the DTO is
 * already what that reader may see.
 */
public record CandidateDossierDto(
		Integer id,
		String email,
		String firstName,
		String lastName,
		String phone,
		LocalDate birthDate,
		Gender gender,
		String city,
		String country,
		LocalDate registrationDate,
		Degree degree,
		boolean hasCv,
		List<EducationDto> education,
		List<CandidateTraitDto> traits,
		List<HrApplicationDto> applications) {
}
