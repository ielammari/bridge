package io.github.ielammari.bridge.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import io.github.ielammari.bridge.dto.CandidateProfileDto;
import io.github.ielammari.bridge.dto.CvDto;
import io.github.ielammari.bridge.dto.EducationRequest;
import io.github.ielammari.bridge.dto.UpdateProfileRequest;
import io.github.ielammari.bridge.exception.ApiException;
import io.github.ielammari.bridge.mapper.ProfileMapper;
import io.github.ielammari.bridge.model.Candidate;
import io.github.ielammari.bridge.model.CandidateTrait;
import io.github.ielammari.bridge.model.Cv;
import io.github.ielammari.bridge.model.Education;
import io.github.ielammari.bridge.model.Trait;
import io.github.ielammari.bridge.repository.CandidateTraitRepository;
import io.github.ielammari.bridge.repository.CvRepository;
import io.github.ielammari.bridge.repository.EducationRepository;
import io.github.ielammari.bridge.repository.TraitRepository;
import io.github.ielammari.bridge.repository.UserRepository;

@Service
public class ProfileService {

	private final UserRepository users;
	private final TraitRepository traits;
	private final CandidateTraitRepository candidateTraits;
	private final EducationRepository education;
	private final CvRepository cvs;
	private final StorageService storage;

	public ProfileService(UserRepository users, TraitRepository traits,
			CandidateTraitRepository candidateTraits, EducationRepository education,
			CvRepository cvs, StorageService storage) {
		this.users = users;
		this.traits = traits;
		this.candidateTraits = candidateTraits;
		this.education = education;
		this.cvs = cvs;
		this.storage = storage;
	}

	@Transactional(readOnly = true)
	public CandidateProfileDto read(Integer candidateId) {
		return profileOf(requireCandidate(candidateId));
	}

	/**
	 * Applies the editable fields and replaces the trait set with exactly the
	 * traits in the request.
	 */
	@Transactional
	public CandidateProfileDto update(Integer candidateId, UpdateProfileRequest request) {
		Candidate candidate = requireCandidate(candidateId);

		candidate.setDegree(request.degree());
		if (request.phone() != null) {
			candidate.setPhone(blankToNull(request.phone()));
		}

		replaceTraits(candidateId, request.traits());

		return profileOf(candidate);
	}

	// ---- The academic path ----------------------------------------------

	@Transactional
	public CandidateProfileDto addEducation(Integer candidateId, EducationRequest request) {
		Candidate candidate = requireCandidate(candidateId);
		checkYears(request);

		education.save(new Education(candidate, request.title().trim(), request.institution().trim(),
				blankToNull(request.fieldOfStudy()), request.startYear(), request.endYear()));

		return profileOf(candidate);
	}

	@Transactional
	public CandidateProfileDto updateEducation(Integer candidateId, Integer educationId,
			EducationRequest request) {
		Candidate candidate = requireCandidate(candidateId);
		checkYears(request);

		Education entry = requireOwned(candidateId, educationId);
		entry.setTitle(request.title().trim());
		entry.setInstitution(request.institution().trim());
		entry.setFieldOfStudy(blankToNull(request.fieldOfStudy()));
		entry.setStartYear(request.startYear());
		entry.setEndYear(request.endYear());

		return profileOf(candidate);
	}

	@Transactional
	public CandidateProfileDto removeEducation(Integer candidateId, Integer educationId) {
		Candidate candidate = requireCandidate(candidateId);
		education.delete(requireOwned(candidateId, educationId));
		return profileOf(candidate);
	}

	/** A qualification cannot end before it began. */
	private void checkYears(EducationRequest request) {
		if (request.endYear() != null && request.endYear() < request.startYear()) {
			throw ApiException.badRequest("INVALID_PERIOD",
					"L'année de fin ne peut pas précéder l'année de début.");
		}
	}

	/** Reading someone else's entry by its id is a not found, never an edit. */
	private Education requireOwned(Integer candidateId, Integer educationId) {
		return education.findById(educationId)
				.filter(entry -> entry.getCandidate().getId().equals(candidateId))
				.orElseThrow(() -> ApiException.notFound("Cette formation est introuvable."));
	}

	private CandidateProfileDto profileOf(Candidate candidate) {
		Integer id = candidate.getId();
		List<CvDto> documents = cvs.findByCandidateIdOrderByUploadedAtDesc(id).stream()
				.map(cv -> new CvDto(cv.getId(), cv.getLabel(), cv.getUploadedAt(),
						cv.getPath().equals(candidate.getCvPath())))
				.toList();
		return ProfileMapper.toProfile(candidate, candidateTraits.findByCandidate(id),
				education.findPath(id), documents);
	}

	// ---- Documents ------------------------------------------------------

	/**
	 * Files a CV under the candidate and makes it the one proposed at apply
	 * time. Earlier documents stay: an application points at the file it was
	 * sent with.
	 */
	@Transactional
	public CandidateProfileDto storeCv(Integer candidateId, MultipartFile file, String label) {
		Candidate candidate = requireCandidate(candidateId);

		String path = storage.storeCv(candidateId, file);
		String name = blankToNull(label);
		cvs.save(new Cv(candidate, name == null ? defaultLabel(candidateId) : name, path));
		candidate.setCvPath(path);

		return profileOf(candidate);
	}

	@Transactional
	public CandidateProfileDto chooseCv(Integer candidateId, Integer cvId) {
		Candidate candidate = requireCandidate(candidateId);
		candidate.setCvPath(requireOwnedCv(candidateId, cvId).getPath());
		return profileOf(candidate);
	}

	@Transactional
	public CandidateProfileDto removeCv(Integer candidateId, Integer cvId) {
		Candidate candidate = requireCandidate(candidateId);
		Cv cv = requireOwnedCv(candidateId, cvId);

		cvs.delete(cv);
		cvs.flush();

		// The default has to land somewhere, so removing the proposed document
		// promotes the most recent of those left.
		if (cv.getPath().equals(candidate.getCvPath())) {
			candidate.setCvPath(cvs.findByCandidateIdOrderByUploadedAtDesc(candidateId).stream()
					.findFirst().map(Cv::getPath).orElse(null));
		}
		storage.deleteCv(cv.getPath());

		return profileOf(candidate);
	}

	/** The path behind one of the candidate's documents, for an application. */
	@Transactional(readOnly = true)
	public String cvPath(Integer candidateId, Integer cvId) {
		return requireOwnedCv(candidateId, cvId).getPath();
	}

	private String defaultLabel(Integer candidateId) {
		return "CV " + (cvs.countByCandidateId(candidateId) + 1);
	}

	private Cv requireOwnedCv(Integer candidateId, Integer cvId) {
		return cvs.findById(cvId)
				.filter(cv -> cv.getCandidate().getId().equals(candidateId))
				.orElseThrow(() -> ApiException.notFound("Ce CV est introuvable."));
	}

	@Transactional(readOnly = true)
	public Resource loadCv(Integer candidateId) {
		Candidate candidate = requireCandidate(candidateId);
		if (candidate.getCvPath() == null) {
			throw ApiException.notFound("Aucun CV n'a été déposé.");
		}
		return storage.loadCv(candidate.getCvPath());
	}

	private void replaceTraits(Integer candidateId, List<UpdateProfileRequest.TraitSelection> selections) {
		candidateTraits.deleteByIdCandidateId(candidateId);
		// The delete and the following inserts share a transaction; flushing the
		// delete first keeps the composite keys from colliding on re-insert.
		candidateTraits.flush();

		if (selections == null || selections.isEmpty()) {
			return;
		}

		// Deduplicate on trait id, preserving the first level given for each.
		Set<Integer> seen = new LinkedHashSet<>();
		List<CandidateTrait> toSave = new ArrayList<>();
		for (UpdateProfileRequest.TraitSelection selection : selections) {
			if (selection.traitId() == null || !seen.add(selection.traitId())) {
				continue;
			}
			Trait trait = traits.findById(selection.traitId())
					.orElseThrow(() -> ApiException.badRequest("UNKNOWN_TRAIT",
							"Une compétence sélectionnée n'existe pas."));
			toSave.add(new CandidateTrait(candidateId, trait, blankToNull(selection.level())));
		}
		candidateTraits.saveAll(toSave);
	}

	private Candidate requireCandidate(Integer candidateId) {
		return users.findById(candidateId)
				.filter(Candidate.class::isInstance)
				.map(Candidate.class::cast)
				.orElseThrow(() -> ApiException.notFound("Ce profil candidat est introuvable."));
	}

	private String blankToNull(String value) {
		return (value == null || value.isBlank()) ? null : value.trim();
	}

}
