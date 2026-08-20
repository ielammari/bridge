package io.github.ielammari.bridge.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.ielammari.bridge.dto.ApplicationTrailDto;
import io.github.ielammari.bridge.dto.AuthoredEvaluationDto;
import io.github.ielammari.bridge.dto.HiringRecordDto;
import io.github.ielammari.bridge.dto.HrApplicationDto;
import io.github.ielammari.bridge.dto.MyApplicationDetailDto;
import io.github.ielammari.bridge.exception.ApiException;
import io.github.ielammari.bridge.mapper.ApplicationMapper;
import io.github.ielammari.bridge.mapper.RecordMapper;
import io.github.ielammari.bridge.model.Application;
import io.github.ielammari.bridge.model.ApplicationStatus;
import io.github.ielammari.bridge.model.Appointment;
import io.github.ielammari.bridge.model.AppointmentType;
import io.github.ielammari.bridge.model.Evaluation;
import io.github.ielammari.bridge.model.Role;
import io.github.ielammari.bridge.repository.ApplicationRepository;
import io.github.ielammari.bridge.repository.AppointmentRepository;
import io.github.ielammari.bridge.repository.EvaluationRepository;
import io.github.ielammari.bridge.repository.HiringRepository;
import io.github.ielammari.bridge.repository.HRInterviewRepository;

/**
 * Reads back what the funnel recorded. The writing side is EvaluationService;
 * nothing here changes an application.
 */
@Service
public class HistoryService {

	private final ApplicationRepository applications;
	private final AppointmentRepository appointments;
	private final EvaluationRepository evaluations;
	private final HRInterviewRepository interviews;
	private final HiringRepository hirings;

	public HistoryService(ApplicationRepository applications, AppointmentRepository appointments,
			EvaluationRepository evaluations, HRInterviewRepository interviews, HiringRepository hirings) {
		this.applications = applications;
		this.appointments = appointments;
		this.evaluations = evaluations;
		this.interviews = interviews;
		this.hirings = hirings;
	}

	/**
	 * The full record of one application, for the actors who ran it. A recruiter
	 * reads what their own offers received; an expert reads one they have
	 * evaluated or one waiting on their exam.
	 */
	@Transactional(readOnly = true)
	public ApplicationTrailDto trail(Integer viewerId, Role viewerRole, Integer applicationId) {
		Application application = require(applicationId);

		if (viewerRole == Role.RH) {
			Ownership.requireOwnApplication(application, viewerId);
		}
		if (viewerRole == Role.EXPERT && !mayExpertRead(viewerId, application)) {
			throw ApiException.notFound("Cette candidature est introuvable.");
		}

		return new ApplicationTrailDto(
				ApplicationMapper.toHrView(application, currentAppointment(application)),
				RecordMapper.toDtos(appointments.findByApplicationIdOrderByDateAscTimeAsc(applicationId)),
				evaluations.findTrail(applicationId).stream().map(RecordMapper::toDto).toList(),
				interviews.findByApplicationId(applicationId).map(RecordMapper::toDto).orElse(null),
				hirings.findByApplicationId(applicationId).map(RecordMapper::toDto).orElse(null));
	}

	/** A candidate's own application in full, carrying no assessment of them. */
	@Transactional(readOnly = true)
	public MyApplicationDetailDto mine(Integer candidateId, Integer applicationId) {
		Application application = require(applicationId);
		if (!application.getCandidate().getId().equals(candidateId)) {
			throw ApiException.notFound("Cette candidature est introuvable.");
		}

		return new MyApplicationDetailDto(
				ApplicationMapper.toCandidateView(application, currentAppointment(application),
						hirings.findByApplicationId(applicationId).map(h -> h.getStartDate()).orElse(null)),
				RecordMapper.toDtos(appointments.findByApplicationIdOrderByDateAscTimeAsc(applicationId)),
				hirings.findByApplicationId(applicationId).map(RecordMapper::toDto).orElse(null));
	}

	/**
	 * Every application that has closed on this recruiter's own offers, in one
	 * query rather than one per offer.
	 */
	@Transactional(readOnly = true)
	public List<HrApplicationDto> closedApplications(Integer hrId) {
		return applications
				.findByStatusInForPublisher(
						List.of(ApplicationStatus.REFUSEE, ApplicationStatus.EMBAUCHEE), hrId)
				.stream()
				.map(application -> ApplicationMapper.toHrView(application, null))
				.toList();
	}

	/**
	 * Every hire in the company, whoever recruited them. A hire joins the
	 * organisation rather than the offer that brought them in, so the register
	 * is not split by recruiter.
	 */
	@Transactional(readOnly = true)
	public List<HiringRecordDto> hires() {
		return hirings.findRegister().stream()
				.map(hiring -> {
					Application application = hiring.getApplication();
					return new HiringRecordDto(
							RecordMapper.toDto(hiring),
							application.getId(),
							application.getCandidate().getId(),
							name(application),
							application.getCandidate().getEmail(),
							application.getOffer().getTitle());
				})
				.toList();
	}

	/**
	 * The evaluations the caller has written, whichever evaluating role they
	 * hold. A recruiter's record follows the offers they run, so one written on
	 * an offer that is not theirs is not listed back to them.
	 */
	@Transactional(readOnly = true)
	public List<AuthoredEvaluationDto> authored(Integer evaluatorId, Role role) {
		List<Evaluation> written = role == Role.RH
				? evaluations.findAuthoredForOwnOffers(evaluatorId)
				: evaluations.findAuthoredBy(evaluatorId);
		return written.stream()
				.map(evaluation -> {
					Application application = evaluation.getApplication();
					return new AuthoredEvaluationDto(
							RecordMapper.toDto(evaluation),
							application.getId(),
							application.getCandidate().getId(),
							name(application),
							application.getOffer().getTitle());
				})
				.toList();
	}

	/** An exam they hold, or one they already sat. */
	private boolean mayExpertRead(Integer expertId, Application application) {
		return appointments.existsByApplicationIdAndEvaluatorId(application.getId(), expertId)
				|| evaluations.existsByApplicationIdAndEvaluatorId(application.getId(), expertId);
	}

	private Application require(Integer applicationId) {
		return applications.findByIdWithOfferAndCandidate(applicationId)
				.orElseThrow(() -> ApiException.notFound("Cette candidature est introuvable."));
	}

	private Appointment currentAppointment(Application application) {
		AppointmentType type = Stages.appointmentTypeFor(application.getStatus());
		return type == null ? null
				: appointments.findByApplicationIdAndType(application.getId(), type).orElse(null);
	}

	private String name(Application application) {
		return application.getCandidate().getFirstName() + " " + application.getCandidate().getLastName();
	}

}
