package io.github.ielammari.bridge.service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.ielammari.bridge.dto.ExaminedTraitDto;
import io.github.ielammari.bridge.dto.FinalEvaluationRequest;
import io.github.ielammari.bridge.dto.HrApplicationDto;
import io.github.ielammari.bridge.dto.PendingTechnicalDto;
import io.github.ielammari.bridge.dto.TechnicalContextDto;
import io.github.ielammari.bridge.dto.TechnicalEvaluationRequest;
import io.github.ielammari.bridge.exception.ApiException;
import io.github.ielammari.bridge.mapper.ApplicationMapper;
import io.github.ielammari.bridge.model.Application;
import io.github.ielammari.bridge.model.ApplicationStatus;
import io.github.ielammari.bridge.model.Appointment;
import io.github.ielammari.bridge.model.AppointmentStatus;
import io.github.ielammari.bridge.model.AppointmentType;
import io.github.ielammari.bridge.model.Decision;
import io.github.ielammari.bridge.model.Evaluation;
import io.github.ielammari.bridge.model.EvaluationType;
import io.github.ielammari.bridge.model.Evaluator;
import io.github.ielammari.bridge.model.HRInterview;
import io.github.ielammari.bridge.model.HRManager;
import io.github.ielammari.bridge.model.Hiring;
import io.github.ielammari.bridge.model.JobOffer;
import io.github.ielammari.bridge.model.OfferRequirement;
import io.github.ielammari.bridge.model.TechnicalExpert;
import io.github.ielammari.bridge.model.Trait;
import io.github.ielammari.bridge.repository.ApplicationRepository;
import io.github.ielammari.bridge.repository.AppointmentRepository;
import io.github.ielammari.bridge.repository.EvaluationRepository;
import io.github.ielammari.bridge.repository.HRInterviewRepository;
import io.github.ielammari.bridge.repository.HiringRepository;
import io.github.ielammari.bridge.repository.JobOfferRepository;
import io.github.ielammari.bridge.repository.TraitRepository;
import io.github.ielammari.bridge.repository.UserRepository;

@Service
public class EvaluationService {

	private final ApplicationRepository applications;
	private final AppointmentRepository appointments;
	private final EvaluationRepository evaluations;
	private final JobOfferRepository offers;
	private final TraitRepository traits;
	private final UserRepository users;
	private final HRInterviewRepository hrInterviews;
	private final HiringRepository hirings;
	private final NotificationService notifications;
	private final StorageService storage;

	public EvaluationService(ApplicationRepository applications, AppointmentRepository appointments,
			EvaluationRepository evaluations, JobOfferRepository offers, TraitRepository traits,
			UserRepository users, HRInterviewRepository hrInterviews, HiringRepository hirings,
			NotificationService notifications, StorageService storage) {
		this.applications = applications;
		this.appointments = appointments;
		this.evaluations = evaluations;
		this.offers = offers;
		this.traits = traits;
		this.users = users;
		this.hrInterviews = hrInterviews;
		this.hirings = hirings;
		this.notifications = notifications;
		this.storage = storage;
	}

	/** HR opens an application to inspect it, which moves it into review. */
	@Transactional
	public HrApplicationDto review(Integer hrId, Integer applicationId) {
		Application application = requireOwnApplication(hrId, applicationId);
		if (application.getStatus() == ApplicationStatus.NOUVELLE) {
			application.setStatus(ApplicationStatus.EN_REVUE);
		}
		return ApplicationMapper.toHrView(application, null);
	}

	/** HR's first screening. Approval advances to the technical exam stage. */
	@Transactional
	public HrApplicationDto preselect(Integer hrId, Integer applicationId, Decision decision, String comment) {
		Evaluator hr = requireHr(hrId);
		Application application = requireOwnApplication(hrId, applicationId);

		if (application.getStatus() != ApplicationStatus.NOUVELLE
				&& application.getStatus() != ApplicationStatus.EN_REVUE) {
			throw ApiException.badRequest("INVALID_STATE",
					"Cette candidature n'est pas au stade de la présélection.");
		}
		if (evaluations.existsByApplicationIdAndType(applicationId, EvaluationType.PRESELECTION)) {
			throw ApiException.badRequest("ALREADY_EVALUATED", "La présélection a déjà été faite.");
		}

		evaluations.save(new Evaluation(EvaluationType.PRESELECTION, decision, blankToNull(comment), application, hr));
		if (decision == Decision.VALIDEE) {
			application.setStatus(ApplicationStatus.EXAMEN_TECHNIQUE);
			notifications.scheduleNeeded(application, AppointmentType.TECHNIQUE);
		} else {
			application.setStatus(ApplicationStatus.REFUSEE);
			notifications.rejected(application);
		}

		return ApplicationMapper.toHrView(application, null);
	}

	/**
	 * The exams waiting on this expert. An exam reaches them by being scheduled
	 * and handed to them, so an unplanned one is nobody's queue yet.
	 */
	@Transactional(readOnly = true)
	public List<PendingTechnicalDto> pendingTechnical(Integer expertId) {
		return appointments.findExamsAssignedTo(expertId).stream()
				.map(appointment -> {
					Application application = appointment.getApplication();
					return new PendingTechnicalDto(
							application.getId(),
							application.getCandidate().getId(),
							application.getCandidate().getFirstName(),
							application.getCandidate().getLastName(),
							application.getOffer().getId(),
							application.getOffer().getTitle(),
							appointment.getDate(),
							appointment.getTime());
				})
				.toList();
	}

	/** The scoring grid: the offer's traits, required first. */
	@Transactional(readOnly = true)
	public TechnicalContextDto technicalContext(Integer expertId, Integer applicationId) {
		Application application = requireApplication(applicationId);
		requireStage(application, ApplicationStatus.EXAMEN_TECHNIQUE);
		requireOwnExam(expertId, applicationId);
		JobOffer offer = requireOfferWithRequirements(application);

		List<ExaminedTraitDto> examined = offer.getRequirements().stream()
				.sorted(Comparator.comparing(OfferRequirement::isMandatory).reversed()
						.thenComparing(r -> r.getTrait().getLabel()))
				.map(r -> new ExaminedTraitDto(r.getTrait().getId(), r.getTrait().getLabel(),
						r.getTrait().getCategory().getLabel(), r.isMandatory()))
				.toList();

		return new TechnicalContextDto(
				application.getId(),
				application.getCandidate().getId(),
				application.getCandidate().getFirstName(),
				application.getCandidate().getLastName(),
				offer.getId(),
				offer.getTitle(),
				examined);
	}

	/** The CV the candidate attached, readable by the expert the exam was handed to. */
	@Transactional(readOnly = true)
	public Resource loadCv(Integer expertId, Integer applicationId) {
		Application application = requireApplication(applicationId);
		requireStage(application, ApplicationStatus.EXAMEN_TECHNIQUE);
		requireOwnExam(expertId, applicationId);
		return storage.loadCv(application.getAttachedCv());
	}

	/** The expert records the technical evaluation. Approval advances to the HR interview. */
	@Transactional
	public void evaluateTechnical(Integer expertId, Integer applicationId, TechnicalEvaluationRequest request) {
		Evaluator expert = requireExpert(expertId);
		Application application = requireApplication(applicationId);
		requireStage(application, ApplicationStatus.EXAMEN_TECHNIQUE);
		Appointment exam = requireOwnExam(expertId, applicationId);
		if (evaluations.existsByApplicationIdAndType(applicationId, EvaluationType.TECHNIQUE)) {
			throw ApiException.badRequest("ALREADY_EVALUATED", "L'évaluation technique a déjà été faite.");
		}

		JobOffer offer = requireOfferWithRequirements(application);
		Set<Integer> offerTraitIds = offer.getRequirements().stream()
				.map(r -> r.getTrait().getId())
				.collect(java.util.stream.Collectors.toSet());

		Evaluation evaluation = new Evaluation(EvaluationType.TECHNIQUE, request.decision(),
				blankToNull(request.comment()), application, expert);

		exam.setStatus(AppointmentStatus.REALISE);
		evaluation.setAppointment(exam);

		Set<Integer> seen = new HashSet<>();
		for (TechnicalEvaluationRequest.Score score : request.scores()) {
			if (!offerTraitIds.contains(score.traitId())) {
				throw ApiException.badRequest("NOT_EXAMINED_TRAIT",
						"Un trait noté ne fait pas partie de l'offre.");
			}
			if (!seen.add(score.traitId())) {
				continue;
			}
			if (score.note() < 0 || score.note() > 10) {
				throw ApiException.badRequest("NOTE_OUT_OF_RANGE", "La note doit être comprise entre 0 et 5 étoiles.");
			}
			Trait trait = traits.findById(score.traitId())
					.orElseThrow(() -> ApiException.badRequest("UNKNOWN_TRAIT", "Un trait noté n'existe pas."));
			evaluation.addScore(trait, score.note());
		}

		evaluations.save(evaluation);
		if (request.decision() == Decision.VALIDEE) {
			application.setStatus(ApplicationStatus.ENTRETIEN_RH);
			notifications.scheduleNeeded(application, AppointmentType.RH);
		} else {
			application.setStatus(ApplicationStatus.REFUSEE);
			notifications.rejected(application);
		}
	}

	/**
	 * HR's final decision, recorded against an interview that was booked: the
	 * assessment reports a meeting, so there must be one. The interview data is
	 * kept whatever the outcome; acceptance creates the hiring record and hires
	 * the candidate, refusal closes the application.
	 */
	@Transactional
	public HrApplicationDto finalize(Integer hrId, Integer applicationId, FinalEvaluationRequest request) {
		Evaluator hr = requireHr(hrId);
		Application application = requireOwnApplication(hrId, applicationId);
		requireStage(application, ApplicationStatus.ENTRETIEN_RH);
		if (evaluations.existsByApplicationIdAndType(applicationId, EvaluationType.ENTRETIEN_RH)) {
			throw ApiException.badRequest("ALREADY_EVALUATED", "L'entretien final a déjà été enregistré.");
		}
		Appointment interview = appointments.findByApplicationIdAndType(applicationId, AppointmentType.RH)
				.orElseThrow(() -> ApiException.badRequest("INTERVIEW_NOT_SCHEDULED",
						"Planifiez l'entretien RH avant de clôturer cette candidature."));

		Evaluation evaluation = new Evaluation(EvaluationType.ENTRETIEN_RH, request.decision(),
				blankToNull(request.comment()), application, hr);
		interview.setStatus(AppointmentStatus.REALISE);
		evaluation.setAppointment(interview);
		evaluations.save(evaluation);

		hrInterviews.save(buildInterview(application, request.interview()));

		if (request.decision() == Decision.VALIDEE) {
			hirings.save(buildHiring(application, request.hiring()));
			application.setStatus(ApplicationStatus.EMBAUCHEE);
			notifications.hired(application);
		} else {
			application.setStatus(ApplicationStatus.REFUSEE);
			notifications.rejected(application);
		}

		return ApplicationMapper.toHrView(application, null);
	}

	private HRInterview buildInterview(Application application, FinalEvaluationRequest.InterviewData data) {
		HRInterview interview = new HRInterview(application);
		interview.setExpectedSalary(data.expectedSalary());
		interview.setAvailabilityDate(data.availabilityDate());
		interview.setEnvisagedContract(data.envisagedContract());
		interview.setNoticePeriod(blankToNull(data.noticePeriod()));
		interview.setScheduleFlexibility(blankToNull(data.scheduleFlexibility()));
		interview.setRemoteExpectation(data.remoteExpectation());
		interview.setCultureFit(blankToNull(data.cultureFit()));
		return interview;
	}

	private Hiring buildHiring(Application application, FinalEvaluationRequest.HiringTerms terms) {
		if (terms == null || terms.negotiatedSalary() == null || terms.startDate() == null
				|| terms.finalContract() == null) {
			throw ApiException.badRequest("HIRING_TERMS_REQUIRED",
					"Le salaire négocié, la date de prise de poste et le contrat final sont obligatoires pour une embauche.");
		}
		Hiring hiring = new Hiring(application, terms.negotiatedSalary(), terms.startDate(), terms.finalContract());
		hiring.setTrialPeriod(blankToNull(terms.trialPeriod()));
		hiring.setExecutiveStatus(terms.executiveStatus());
		hiring.setBenefits(blankToNull(terms.benefits()));
		return hiring;
	}

	private JobOffer requireOfferWithRequirements(Application application) {
		return offers.findByIdWithRequirements(application.getOffer().getId())
				.orElseThrow(() -> ApiException.notFound("L'offre de cette candidature est introuvable."));
	}

	private void requireStage(Application application, ApplicationStatus expected) {
		if (application.getStatus() != expected) {
			throw ApiException.badRequest("INVALID_STATE", "Cette candidature n'est pas au bon stade.");
		}
	}

	private Application requireApplication(Integer applicationId) {
		return applications.findByIdWithOfferAndCandidate(applicationId)
				.orElseThrow(() -> ApiException.notFound("Cette candidature est introuvable."));
	}

	/** A recruiter acts only on the applications their own offers received. */
	private Application requireOwnApplication(Integer hrId, Integer applicationId) {
		return Ownership.requireOwnApplication(requireApplication(applicationId), hrId);
	}

	/**
	 * The exam this expert was handed. An exam that was never scheduled, or that
	 * went to somebody else, is not theirs to sit or to read.
	 */
	private Appointment requireOwnExam(Integer expertId, Integer applicationId) {
		Appointment exam = appointments
				.findByApplicationIdAndType(applicationId, AppointmentType.TECHNIQUE)
				.orElseThrow(() -> ApiException.notFound("Cet examen technique est introuvable."));
		if (!exam.getEvaluator().getId().equals(expertId)) {
			throw ApiException.notFound("Cet examen technique est introuvable.");
		}
		return exam;
	}

	private Evaluator requireHr(Integer id) {
		return users.findById(id)
				.filter(HRManager.class::isInstance)
				.map(Evaluator.class::cast)
				.orElseThrow(() -> ApiException.notFound("Ce responsable RH est introuvable."));
	}

	private Evaluator requireExpert(Integer id) {
		return users.findById(id)
				.filter(TechnicalExpert.class::isInstance)
				.map(Evaluator.class::cast)
				.orElseThrow(() -> ApiException.notFound("Cet expert technique est introuvable."));
	}

	private String blankToNull(String value) {
		return (value == null || value.isBlank()) ? null : value.trim();
	}

}
