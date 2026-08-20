package io.github.ielammari.bridge.service;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.ielammari.bridge.dto.CandidateDossierDto;
import io.github.ielammari.bridge.exception.ApiException;
import io.github.ielammari.bridge.mapper.ApplicationMapper;
import io.github.ielammari.bridge.mapper.ProfileMapper;
import io.github.ielammari.bridge.mapper.TraitMapper;
import io.github.ielammari.bridge.model.Application;
import io.github.ielammari.bridge.model.Candidate;
import io.github.ielammari.bridge.model.Role;
import io.github.ielammari.bridge.repository.ApplicationRepository;
import io.github.ielammari.bridge.repository.AppointmentRepository;
import io.github.ielammari.bridge.repository.CandidateTraitRepository;
import io.github.ielammari.bridge.repository.EducationRepository;
import io.github.ielammari.bridge.repository.EvaluationRepository;
import io.github.ielammari.bridge.repository.UserRepository;

/**
 * One candidate at their own address, for whoever is entitled to open them: a
 * recruiter for the people who applied to their own offers, an expert for the
 * people they hold an exam for or have assessed, a candidate for themselves.
 * Anyone else is told the person does not exist, so the address cannot be used
 * to discover who does.
 */
@Service
public class PeopleService {

	private final UserRepository users;
	private final ApplicationRepository applications;
	private final AppointmentRepository appointments;
	private final CandidateTraitRepository candidateTraits;
	private final EducationRepository education;
	private final EvaluationRepository evaluations;
	private final StorageService storage;

	public PeopleService(UserRepository users, ApplicationRepository applications,
			AppointmentRepository appointments, CandidateTraitRepository candidateTraits,
			EducationRepository education, EvaluationRepository evaluations, StorageService storage) {
		this.users = users;
		this.applications = applications;
		this.appointments = appointments;
		this.candidateTraits = candidateTraits;
		this.education = education;
		this.evaluations = evaluations;
		this.storage = storage;
	}

	@Transactional(readOnly = true)
	public CandidateDossierDto dossier(Integer viewerId, Role viewerRole, Integer candidateId) {
		Candidate candidate = readable(viewerId, viewerRole, candidateId);

		return new CandidateDossierDto(
				candidate.getId(),
				candidate.getEmail(),
				candidate.getFirstName(),
				candidate.getLastName(),
				candidate.getPhone(),
				candidate.getBirthDate(),
				candidate.getGender(),
				candidate.getCity(),
				candidate.getCountry(),
				candidate.getRegistrationDate(),
				candidate.getDegree(),
				candidate.getCvPath() != null,
				education.findPath(candidateId).stream().map(ProfileMapper::toDto).toList(),
				candidateTraits.findByCandidate(candidateId).stream().map(TraitMapper::toDto).toList(),
				visibleApplications(viewerId, viewerRole, candidateId).stream()
						.map(application -> ApplicationMapper.toHrView(application, null)).toList());
	}

	/**
	 * The dossier is built for the reader: an expert's entitlement stops at the
	 * applications they ran or were handed, a recruiter's at the ones their own
	 * offers received.
	 */
	private List<Application> visibleApplications(Integer viewerId, Role viewerRole, Integer candidateId) {
		return switch (viewerRole) {
			case EXPERT -> applications.findByCandidateAssessedBy(candidateId, viewerId);
			case RH -> applications.findByCandidateForPublisher(candidateId, viewerId);
			case CANDIDAT -> applications.findByCandidate(candidateId);
		};
	}

	/** The CV behind the dossier, on the same entitlement as the dossier. */
	@Transactional(readOnly = true)
	public Resource cv(Integer viewerId, Role viewerRole, Integer candidateId) {
		Candidate candidate = readable(viewerId, viewerRole, candidateId);
		if (candidate.getCvPath() == null) {
			throw ApiException.notFound("Aucun CV n'a été déposé.");
		}
		return storage.loadCv(candidate.getCvPath());
	}

	private Candidate readable(Integer viewerId, Role viewerRole, Integer candidateId) {
		boolean allowed = switch (viewerRole) {
			case RH -> applications.existsByCandidateAndPublisher(candidateId, viewerId);
			case EXPERT -> evaluations.hasAssessed(viewerId, candidateId)
					|| appointments.existsByApplicationCandidateIdAndEvaluatorId(candidateId, viewerId);
			case CANDIDAT -> viewerId.equals(candidateId);
		};
		if (!allowed) {
			throw ApiException.notFound("Cette personne est introuvable.");
		}

		return users.findById(candidateId)
				.filter(Candidate.class::isInstance)
				.map(Candidate.class::cast)
				.orElseThrow(() -> ApiException.notFound("Cette personne est introuvable."));
	}

}
