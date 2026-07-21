package io.github.ielammari.bridge.mapper;

import java.util.List;

import io.github.ielammari.bridge.dto.CandidateProfileDto;
import io.github.ielammari.bridge.dto.CandidateTraitDto;
import io.github.ielammari.bridge.model.Candidate;
import io.github.ielammari.bridge.model.CandidateTrait;

/** Assembles the candidate profile view from the candidate and its traits. */
public final class ProfileMapper {

	private ProfileMapper() {
	}

	public static CandidateProfileDto toProfile(Candidate candidate, List<CandidateTrait> traits) {
		List<CandidateTraitDto> traitDtos = traits.stream()
				.map(TraitMapper::toDto)
				.toList();

		return new CandidateProfileDto(
				candidate.getId(),
				candidate.getEmail(),
				candidate.getFirstName(),
				candidate.getLastName(),
				candidate.getPhone(),
				candidate.getDegree(),
				candidate.getExperienceLevel(),
				candidate.getCvPath() != null,
				traitDtos);
	}

}
