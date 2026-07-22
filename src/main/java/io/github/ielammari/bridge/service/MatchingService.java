package io.github.ielammari.bridge.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.ielammari.bridge.dto.OfferDto;
import io.github.ielammari.bridge.exception.ApiException;
import io.github.ielammari.bridge.mapper.OfferMapper;
import io.github.ielammari.bridge.model.Candidate;
import io.github.ielammari.bridge.model.JobOffer;
import io.github.ielammari.bridge.model.OfferRequirement;
import io.github.ielammari.bridge.model.OfferStatus;
import io.github.ielammari.bridge.repository.CandidateTraitRepository;
import io.github.ielammari.bridge.repository.JobOfferRepository;
import io.github.ielammari.bridge.repository.UserRepository;

/**
 * Decides which published offers a candidate qualifies for. An offer is
 * compatible when the candidate holds every required trait and meets the degree
 * requirement; plus traits never affect visibility.
 */
@Service
public class MatchingService {

	private final JobOfferRepository offers;
	private final UserRepository users;
	private final CandidateTraitRepository candidateTraits;

	public MatchingService(JobOfferRepository offers, UserRepository users,
			CandidateTraitRepository candidateTraits) {
		this.offers = offers;
		this.users = users;
		this.candidateTraits = candidateTraits;
	}

	@Transactional(readOnly = true)
	public List<OfferDto> feed(Integer candidateId) {
		Candidate candidate = requireCandidate(candidateId);
		Set<Integer> held = heldTraitIds(candidateId);

		return offers.findByStatusWithRequirements(OfferStatus.PUBLIEE).stream()
				.filter(offer -> isCompatible(candidate, held, offer))
				.map(OfferMapper::toDto)
				.toList();
	}

	/**
	 * Compatibility gate. Kept package visible and pure so the unit tests can
	 * exercise it directly with hand built inputs.
	 */
	boolean isCompatible(Candidate candidate, Set<Integer> heldTraitIds, JobOffer offer) {
		if (candidate.getDegree() == null || !candidate.getDegree().satisfies(offer.getRequiredDegree())) {
			return false;
		}
		return offer.getRequirements().stream()
				.filter(OfferRequirement::isMandatory)
				.map(requirement -> requirement.getTrait().getId())
				.allMatch(heldTraitIds::contains);
	}

	private Set<Integer> heldTraitIds(Integer candidateId) {
		return candidateTraits.findByCandidate(candidateId).stream()
				.map(candidateTrait -> candidateTrait.getTrait().getId())
				.collect(Collectors.toSet());
	}

	private Candidate requireCandidate(Integer candidateId) {
		return users.findById(candidateId)
				.filter(Candidate.class::isInstance)
				.map(Candidate.class::cast)
				.orElseThrow(() -> ApiException.notFound("Ce profil candidat est introuvable."));
	}

}
