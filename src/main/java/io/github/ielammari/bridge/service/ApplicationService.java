package io.github.ielammari.bridge.service;

import java.time.LocalDate;
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
import io.github.ielammari.bridge.model.ApplicationStatus;
import io.github.ielammari.bridge.model.Hiring;
import io.github.ielammari.bridge.repository.ApplicationRepository;
import io.github.ielammari.bridge.repository.AppointmentRepository;
import io.github.ielammari.bridge.repository.HiringRepository;
import io.github.ielammari.bridge.repository.JobOfferRepository;
import io.github.ielammari.bridge.repository.UserRepository;

@Service
public class ApplicationService {

	private final ApplicationRepository applications;
	private final AppointmentRepository appointments;
	private final HiringRepository hirings;
	private final JobOfferRepository offers;
	private final UserRepository users;
	private final MatchingService matching;
	private final StorageService storage;
	private final NotificationService notifications;
	private final ProfileService profiles;

	public ApplicationService(ApplicationRepository applications, AppointmentRepository appointments,
			HiringRepository hirings, JobOfferRepository offers, UserRepository users,
			MatchingService matching, StorageService storage, NotificationService notifications,
			ProfileService profiles) {
		this.applications = applications;
		this.appointments = appointments;
		this.hirings = hirings;
		this.offers = offers;
		this.users = users;
		this.matching = matching;
		this.storage = storage;
		this.notifications = notifications;
		this.profiles = profiles;
	}

	/**
	 * Applies the candidate to the offer. The gates run in the order the
	 * sequence diagram fixes: the offer must be open and compatible, then not
	 * already applied to, then the candidate must have a CV.
	 */
	@Transactional
	public ApplicationDto apply(Integer candidateId, Integer offerId, Integer cvId) {
		Candidate candidate = requireCandidate(candidateId);
		JobOffer offer = offers.findByIdWithRequirements(offerId)
				.orElseThrow(() -> ApiException.notFound("Cette offre est introuvable."));

		if (offer.getStatus() != OfferStatus.PUBLIEE || !matching.isCompatible(candidate, offer)) {
			throw ApiException.badRequest("OFFER_NOT_ACCESSIBLE",
					"Cette offre n'est pas accessible avec votre profil.");
		}
		// A refusal closes an attempt without closing the door: only a live
		// application blocks another.
		if (applications.existsByCandidateIdAndOfferIdAndStatusNot(candidateId, offerId,
				ApplicationStatus.REFUSEE)) {
			throw ApiException.badRequest("ALREADY_APPLIED",
					"Vous avez déjà une candidature en cours pour cette offre.");
		}
		String cv = cvId == null ? candidate.getCvPath() : profiles.cvPath(candidateId, cvId);
		if (cv == null) {
			throw ApiException.badRequest("CV_REQUIRED",
					"Ajoutez un CV à votre profil avant de postuler.");
		}

		// The path is copied onto the application, so replacing the document
		// later never changes what the recruiter received.
		Application application = applications.save(new Application(candidate, offer, cv));
		notifications.applicationReceived(application);
		return ApplicationMapper.toCandidateView(application, null, null);
	}

	@Transactional(readOnly = true)
	public List<ApplicationDto> forCandidate(Integer candidateId) {
		return applications.findByCandidate(candidateId).stream()
				.map(a -> ApplicationMapper.toCandidateView(a, currentAppointment(a), hiringStartDate(a)))
				.toList();
	}

	private LocalDate hiringStartDate(Application application) {
		if (application.getStatus() != ApplicationStatus.EMBAUCHEE) {
			return null;
		}
		return hirings.findByApplicationId(application.getId()).map(Hiring::getStartDate).orElse(null);
	}

	/** The applications to one of this recruiter's own offers. */
	@Transactional(readOnly = true)
	public List<HrApplicationDto> forOffer(Integer hrId, Integer offerId) {
		requireOwnOffer(hrId, offerId);
		return applications.findByOffer(offerId).stream()
				.map(a -> ApplicationMapper.toHrView(a, currentAppointment(a)))
				.toList();
	}

	/** One application in the HR view, so a review in progress can be linked to. */
	@Transactional(readOnly = true)
	public HrApplicationDto hrView(Integer hrId, Integer applicationId) {
		Application application = requireOwnApplication(hrId, applicationId);
		return ApplicationMapper.toHrView(application, currentAppointment(application));
	}

	/** The interview an application is currently waiting on, or null if none applies. */
	private Appointment currentAppointment(Application application) {
		AppointmentType type = Stages.appointmentTypeFor(application.getStatus());
		return type == null ? null
				: appointments.findByApplicationIdAndType(application.getId(), type).orElse(null);
	}

	/** Serves the CV attached to an application, for HR review. */
	@Transactional(readOnly = true)
	public Resource loadCv(Integer hrId, Integer applicationId) {
		return storage.loadCv(requireOwnApplication(hrId, applicationId).getAttachedCv());
	}

	private void requireOwnOffer(Integer hrId, Integer offerId) {
		Ownership.requireOwnOffer(offers.findById(offerId)
				.orElseThrow(() -> ApiException.notFound("Cette offre est introuvable.")), hrId);
	}

	private Application requireOwnApplication(Integer hrId, Integer applicationId) {
		return Ownership.requireOwnApplication(applications.findByIdWithOfferAndCandidate(applicationId)
				.orElseThrow(() -> ApiException.notFound("Cette candidature est introuvable.")), hrId);
	}

	private Candidate requireCandidate(Integer candidateId) {
		return users.findById(candidateId)
				.filter(Candidate.class::isInstance)
				.map(Candidate.class::cast)
				.orElseThrow(() -> ApiException.notFound("Ce profil candidat est introuvable."));
	}

}
