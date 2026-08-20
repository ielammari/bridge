package io.github.ielammari.bridge.dto;

import java.util.List;

import io.github.ielammari.bridge.model.Degree;

/**
 * The full candidate profile returned to the profile page. `degree` is the
 * scalar level the matching gate reads; `education` is the path behind it.
 */
public record CandidateProfileDto(
		Integer id,
		String email,
		String firstName,
		String lastName,
		String phone,
		Degree degree,
		boolean hasCv,
		List<CvDto> cvs,
		List<EducationDto> education,
		List<CandidateTraitDto> traits) {
}
