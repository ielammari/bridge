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
import io.github.ielammari.bridge.dto.UpdateProfileRequest;
import io.github.ielammari.bridge.exception.ApiException;
import io.github.ielammari.bridge.mapper.ProfileMapper;
import io.github.ielammari.bridge.model.Candidate;
import io.github.ielammari.bridge.model.CandidateTrait;
import io.github.ielammari.bridge.model.Trait;
import io.github.ielammari.bridge.repository.CandidateTraitRepository;
import io.github.ielammari.bridge.repository.TraitRepository;
import io.github.ielammari.bridge.repository.UserRepository;

@Service
public class ProfileService {

	private final UserRepository users;
	private final TraitRepository traits;
	private final CandidateTraitRepository candidateTraits;
	private final StorageService storage;

	public ProfileService(UserRepository users, TraitRepository traits,
			CandidateTraitRepository candidateTraits, StorageService storage) {
		this.users = users;
		this.traits = traits;
		this.candidateTraits = candidateTraits;
		this.storage = storage;
	}

	@Transactional(readOnly = true)
	public CandidateProfileDto read(Integer candidateId) {
		Candidate candidate = requireCandidate(candidateId);
		return ProfileMapper.toProfile(candidate, candidateTraits.findByCandidate(candidateId));
	}

	/**
	 * Applies the editable fields and replaces the trait set with exactly the
	 * traits in the request.
	 */
	@Transactional
	public CandidateProfileDto update(Integer candidateId, UpdateProfileRequest request) {
		Candidate candidate = requireCandidate(candidateId);

		candidate.setDegree(request.degree());
		candidate.setExperienceLevel(blankToNull(request.experienceLevel()));
		if (request.phone() != null) {
			candidate.setPhone(blankToNull(request.phone()));
		}

		replaceTraits(candidateId, request.traits());

		return ProfileMapper.toProfile(candidate, candidateTraits.findByCandidate(candidateId));
	}

	@Transactional
	public void storeCv(Integer candidateId, MultipartFile file) {
		Candidate candidate = requireCandidate(candidateId);
		String previous = candidate.getCvPath();

		candidate.setCvPath(storage.storeCv(candidateId, file));

		// The new file is committed to the row, so the old one can go.
		if (previous != null) {
			storage.deleteCv(previous);
		}
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
