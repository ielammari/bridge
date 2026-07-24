package io.github.ielammari.bridge.service;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.ielammari.bridge.dto.ApplicationDto;
import io.github.ielammari.bridge.dto.HrApplicationDto;
import io.github.ielammari.bridge.exception.ApiException;
import io.github.ielammari.bridge.mapper.ApplicationMapper;
import io.github.ielammari.bridge.model.Application;
import io.github.ielammari.bridge.model.Appointment;
import io.github.ielammari.bridge.model.AppointmentType;
import io.github.ielammari.bridge.model.Candidate;
import io.github.ielammari.bridge.model.JobOffer;
import io.github.ielammari.bridge.model.OfferStatus;
import io.github.ielammari.bridge.repository.ApplicationRepository;
import io.github.ielammari.bridge.repository.AppointmentRepository;
import io.github.ielammari.bridge.repository.JobOfferRepository;
import io.github.ielammari.bridge.repository.UserRepository;

@Service
public class ApplicationService {

	private final ApplicationRepository applications;
	private final AppointmentRepository appointments;
	private final JobOfferRepository offers;
	private final UserRepository users;
	private final MatchingService matching;
	private final StorageService storage;

	public ApplicationService(ApplicationRepository applications, AppointmentRepository appointments,
			JobOfferRepository offers, UserRepository users, MatchingService matching, StorageService storage) {
		this.applications = applications;
		this.appointments = appointments;
		this.offers = offers;
		this.users = users;
		this.matching = matching;
		this.storage = storage;
	}

	/**
	 * Applies the candidate to the offer. The gates run in the order the
	 * sequence diagram fixes: the offer must be open and compatible, then not
	 * already applied to, then the candidate must have a CV.
	 */
	@Transactional
	public ApplicationDto apply(Integer candidateId, Integer offerId) {
		Candidate candidate = requireCandidate(candidateId);
		JobOffer offer = offers.findByIdWithRequirements(offerId)
				.orElseThrow(() -> ApiException.notFound("Cette offre est introuvable."));

		if (offer.getStatus() != OfferStatus.PUBLIEE || !matching.isCompatible(candidate, offer)) {
			throw ApiException.badRequest("OFFER_NOT_ACCESSIBLE",
					"Cette offre n'est pas accessible avec votre profil.");
		}
		if (applications.existsByCandidateIdAndOfferId(candidateId, offerId)) {
			throw ApiException.badRequest("ALREADY_APPLIED", "Vous avez déjà postulé à cette offre.");
		}
		if (candidate.getCvPath() == null) {
			throw ApiException.badRequest("CV_REQUIRED",
					"Ajoutez un CV à votre profil avant de postuler.");
		}

		Application application = new Application(candidate, offer, candidate.getCvPath());
		return ApplicationMapper.toCandidateView(applications.save(application), null);
	}

	@Transactional(readOnly = true)
	public List<ApplicationDto> forCandidate(Integer candidateId) {
		return applications.findByCandidate(candidateId).stream()
				.map(a -> ApplicationMapper.toCandidateView(a, currentAppointment(a)))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<HrApplicationDto> forOffer(Integer offerId) {
		return applications.findByOffer(offerId).stream()
				.map(a -> ApplicationMapper.toHrView(a, currentAppointment(a)))
				.toList();
	}

	/** The interview an application is currently waiting on, or null if none applies. */
	private Appointment currentAppointment(Application application) {
		AppointmentType type = Stages.appointmentTypeFor(application.getStatus());
		return type == null ? null
				: appointments.findByApplicationIdAndType(application.getId(), type).orElse(null);
	}

	/** Serves the CV attached to an application, for HR review. */
	@Transactional(readOnly = true)
	public Resource loadCv(Integer applicationId) {
		Application application = applications.findByIdWithOfferAndCandidate(applicationId)
				.orElseThrow(() -> ApiException.notFound("Cette candidature est introuvable."));
		return storage.loadCv(application.getAttachedCv());
	}

	private Candidate requireCandidate(Integer candidateId) {
		return users.findById(candidateId)
				.filter(Candidate.class::isInstance)
				.map(Candidate.class::cast)
				.orElseThrow(() -> ApiException.notFound("Ce profil candidat est introuvable."));
	}

}
