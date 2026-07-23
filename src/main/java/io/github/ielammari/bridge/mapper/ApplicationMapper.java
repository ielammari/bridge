package io.github.ielammari.bridge.mapper;

import io.github.ielammari.bridge.dto.ApplicationDto;
import io.github.ielammari.bridge.dto.HrApplicationDto;
import io.github.ielammari.bridge.model.Application;
import io.github.ielammari.bridge.model.Candidate;
import io.github.ielammari.bridge.model.JobOffer;

/** Pure transformations from an application to its candidate and HR views. */
public final class ApplicationMapper {

	private ApplicationMapper() {
	}

	public static ApplicationDto toCandidateView(Application application) {
		JobOffer offer = application.getOffer();
		return new ApplicationDto(
				application.getId(),
				offer.getId(),
				offer.getTitle(),
				offer.getContractType(),
				offer.getLocation(),
				application.getApplicationDate(),
				application.getStatus());
	}

	public static HrApplicationDto toHrView(Application application) {
		Candidate candidate = application.getCandidate();
		return new HrApplicationDto(
				application.getId(),
				candidate.getId(),
				candidate.getFirstName(),
				candidate.getLastName(),
				candidate.getEmail(),
				application.getApplicationDate(),
				application.getStatus());
	}

}
