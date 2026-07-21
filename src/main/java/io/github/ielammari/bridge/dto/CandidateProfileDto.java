package io.github.ielammari.bridge.dto;

import java.util.List;

import io.github.ielammari.bridge.model.Degree;

/** The full candidate profile returned to the profile page. */
public record CandidateProfileDto(
		Integer id,
		String email,
		String firstName,
		String lastName,
		String phone,
		Degree degree,
		String experienceLevel,
		boolean hasCv,
		List<CandidateTraitDto> traits) {
}
