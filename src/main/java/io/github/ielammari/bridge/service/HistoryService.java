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
	 * The full record of one application, for the actors who ran it.
	 *
	 * An expert sees only the applications they are involved in: one they have
	 * already evaluated, or one currently waiting on their exam.
	 */
	@Transactional(readOnly = true)
	public ApplicationTrailDto trail(Integer viewerId, Role viewerRole, Integer applicationId) {
		Application application = require(applicationId);

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
	 * Every application that has closed, across every offer. Gathering these one
	 * offer at a time costs a request per offer, which the history page would pay
	 * on every visit.
	 */
	@Transactional(readOnly = true)
	public List<HrApplicationDto> closedApplications() {
		return applications.findByStatusIn(List.of(ApplicationStatus.REFUSEE, ApplicationStatus.EMBAUCHEE))
				.stream()
				.map(application -> ApplicationMapper.toHrView(application, null))
				.toList();
	}

	/** Every hire, for HR. */
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

	/** The evaluations the caller has written, whichever evaluating role they hold. */
	@Transactional(readOnly = true)
	public List<AuthoredEvaluationDto> authored(Integer evaluatorId) {
		return evaluations.findAuthoredBy(evaluatorId).stream()
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

	private boolean mayExpertRead(Integer expertId, Application application) {
		return application.getStatus() == ApplicationStatus.EXAMEN_TECHNIQUE
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
