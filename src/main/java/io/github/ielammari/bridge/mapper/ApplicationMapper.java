package io.github.ielammari.bridge.mapper;

import java.time.LocalDate;

import io.github.ielammari.bridge.dto.ApplicationDto;
import io.github.ielammari.bridge.dto.HrApplicationDto;
import io.github.ielammari.bridge.model.Application;
import io.github.ielammari.bridge.model.Appointment;
import io.github.ielammari.bridge.model.Candidate;
import io.github.ielammari.bridge.model.JobOffer;

/** Pure transformations from an application to its candidate and HR views. */
public final class ApplicationMapper {

	private ApplicationMapper() {
	}

	public static ApplicationDto toCandidateView(Application application, Appointment appointment,
			LocalDate hiringStartDate) {
		JobOffer offer = application.getOffer();
		return new ApplicationDto(
				application.getId(),
				offer.getId(),
				offer.getTitle(),
				offer.getContractType(),
				offer.getLocation(),
				application.getApplicationDate(),
				application.getStatus(),
				appointment == null ? null : appointment.getDate(),
				appointment == null ? null : appointment.getTime(),
				hiringStartDate);
	}

	public static HrApplicationDto toHrView(Application application, Appointment appointment) {
		Candidate candidate = application.getCandidate();
		return new HrApplicationDto(
				application.getId(),
				candidate.getId(),
				candidate.getFirstName(),
				candidate.getLastName(),
				candidate.getEmail(),
				application.getOffer().getId(),
				application.getOffer().getTitle(),
				application.getApplicationDate(),
				application.getStatus(),
				appointment == null ? null : appointment.getDate(),
				appointment == null ? null : appointment.getTime(),
				appointment == null ? null : appointment.getEvaluator().getId(),
				appointment == null ? null
						: appointment.getEvaluator().getFirstName() + " "
								+ appointment.getEvaluator().getLastName(),
				application.getOffer().isWaitForAppointment());
	}

}
