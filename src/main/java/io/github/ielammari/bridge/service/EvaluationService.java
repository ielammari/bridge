package io.github.ielammari.bridge.service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.ielammari.bridge.dto.ExaminedTraitDto;
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
import io.github.ielammari.bridge.model.HRManager;
import io.github.ielammari.bridge.model.JobOffer;
import io.github.ielammari.bridge.model.OfferRequirement;
import io.github.ielammari.bridge.model.TechnicalExpert;
import io.github.ielammari.bridge.model.Trait;
import io.github.ielammari.bridge.repository.ApplicationRepository;
import io.github.ielammari.bridge.repository.AppointmentRepository;
import io.github.ielammari.bridge.repository.EvaluationRepository;
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

	public EvaluationService(ApplicationRepository applications, AppointmentRepository appointments,
			EvaluationRepository evaluations, JobOfferRepository offers, TraitRepository traits,
			UserRepository users) {
		this.applications = applications;
		this.appointments = appointments;
		this.evaluations = evaluations;
		this.offers = offers;
		this.traits = traits;
		this.users = users;
	}

	/** HR opens an application to inspect it, which moves it into review. */
	@Transactional
	public HrApplicationDto review(Integer applicationId) {
		Application application = requireApplication(applicationId);
		if (application.getStatus() == ApplicationStatus.NOUVELLE) {
			application.setStatus(ApplicationStatus.EN_REVUE);
		}
		return ApplicationMapper.toHrView(application, null);
	}

	/** HR's first screening. Approval advances to the technical exam stage. */
	@Transactional
	public HrApplicationDto preselect(Integer hrId, Integer applicationId, Decision decision, String comment) {
		Evaluator hr = requireHr(hrId);
		Application application = requireApplication(applicationId);

		if (application.getStatus() != ApplicationStatus.NOUVELLE
				&& application.getStatus() != ApplicationStatus.EN_REVUE) {
			throw ApiException.badRequest("INVALID_STATE",
					"Cette candidature n'est pas au stade de la présélection.");
		}
		if (evaluations.existsByApplicationIdAndType(applicationId, EvaluationType.PRESELECTION)) {
			throw ApiException.badRequest("ALREADY_EVALUATED", "La présélection a déjà été faite.");
		}

		evaluations.save(new Evaluation(EvaluationType.PRESELECTION, decision, blankToNull(comment), application, hr));
		application.setStatus(decision == Decision.VALIDEE
				? ApplicationStatus.EXAMEN_TECHNIQUE
				: ApplicationStatus.REFUSEE);

		return ApplicationMapper.toHrView(application, null);
	}

	@Transactional(readOnly = true)
	public List<PendingTechnicalDto> pendingTechnical() {
		return applications.findByStatusWithCandidateAndOffer(ApplicationStatus.EXAMEN_TECHNIQUE).stream()
				.map(application -> {
					Appointment appointment = appointments
							.findByApplicationIdAndType(application.getId(), AppointmentType.TECHNIQUE)
							.orElse(null);
					return new PendingTechnicalDto(
							application.getId(),
							application.getCandidate().getFirstName(),
							application.getCandidate().getLastName(),
							application.getOffer().getTitle(),
							appointment == null ? null : appointment.getDate(),
							appointment == null ? null : appointment.getTime());
				})
				.toList();
	}

	/** The scoring grid: the offer's traits, required first. */
	@Transactional(readOnly = true)
	public TechnicalContextDto technicalContext(Integer applicationId) {
		Application application = requireApplication(applicationId);
		requireStage(application, ApplicationStatus.EXAMEN_TECHNIQUE);
		JobOffer offer = requireOfferWithRequirements(application);

		List<ExaminedTraitDto> examined = offer.getRequirements().stream()
				.sorted(Comparator.comparing(OfferRequirement::isMandatory).reversed()
						.thenComparing(r -> r.getTrait().getLabel()))
				.map(r -> new ExaminedTraitDto(r.getTrait().getId(), r.getTrait().getLabel(),
						r.getTrait().getCategory().getLabel(), r.isMandatory()))
				.toList();

		return new TechnicalContextDto(
				application.getId(),
				application.getCandidate().getFirstName(),
				application.getCandidate().getLastName(),
				offer.getTitle(),
				examined);
	}

	/** The expert records the technical evaluation. Approval advances to the HR interview. */
	@Transactional
	public void evaluateTechnical(Integer expertId, Integer applicationId, TechnicalEvaluationRequest request) {
		Evaluator expert = requireExpert(expertId);
		Application application = requireApplication(applicationId);
		requireStage(application, ApplicationStatus.EXAMEN_TECHNIQUE);
		if (evaluations.existsByApplicationIdAndType(applicationId, EvaluationType.TECHNIQUE)) {
			throw ApiException.badRequest("ALREADY_EVALUATED", "L'évaluation technique a déjà été faite.");
		}

		JobOffer offer = requireOfferWithRequirements(application);
		Set<Integer> offerTraitIds = offer.getRequirements().stream()
				.map(r -> r.getTrait().getId())
				.collect(java.util.stream.Collectors.toSet());

		Evaluation evaluation = new Evaluation(EvaluationType.TECHNIQUE, request.decision(),
				blankToNull(request.comment()), application, expert);

		// Link to the technical appointment and mark it held, if it was scheduled.
		appointments.findByApplicationIdAndType(applicationId, AppointmentType.TECHNIQUE)
				.ifPresent(appointment -> {
					appointment.setStatus(AppointmentStatus.REALISE);
					evaluation.setAppointment(appointment);
				});

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
		application.setStatus(request.decision() == Decision.VALIDEE
				? ApplicationStatus.ENTRETIEN_RH
				: ApplicationStatus.REFUSEE);
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
