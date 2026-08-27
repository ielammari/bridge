package io.github.ielammari.bridge.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.ielammari.bridge.dto.MatchDto;
import io.github.ielammari.bridge.dto.OfferMatchDto;
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

	/**
	 * The published offers, each carrying where the candidate stands against it.
	 * {@code compatibleOnly} drops the ones they do not qualify for; keeping them
	 * lets the feed show the whole market without opening it to application.
	 */
	@Transactional(readOnly = true)
	public List<OfferMatchDto> feed(Integer candidateId, boolean compatibleOnly) {
		return describe(candidateId, offers.findByStatusWithRequirements(OfferStatus.PUBLIEE)).stream()
				.filter(entry -> !compatibleOnly || entry.match().compatible())
				.toList();
	}

	/**
	 * The same reading for a list somebody else assembled. The candidate and
	 * their traits are read once, whatever the length of the list.
	 */
	@Transactional(readOnly = true)
	public List<OfferMatchDto> describe(Integer candidateId, List<JobOffer> subjects) {
		Candidate candidate = requireCandidate(candidateId);
		Set<Integer> held = heldTraitIds(candidateId);

		return subjects.stream()
				.map(offer -> new OfferMatchDto(OfferMapper.toDto(offer), match(candidate, held, offer)))
				.toList();
	}

	/** Where the candidate stands against one offer, for a page rather than a listing. */
	@Transactional(readOnly = true)
	public MatchDto match(Candidate candidate, JobOffer offer) {
		return match(candidate, heldTraitIds(candidate.getId()), offer);
	}

	/** Whether the candidate qualifies for the given offer, gating application. */
	@Transactional(readOnly = true)
	public boolean isCompatible(Candidate candidate, JobOffer offer) {
		return match(candidate, offer).compatible();
	}

	/**
	 * The compatibility gate, and the reasons it fails. Kept package visible and
	 * pure so the unit tests can exercise it directly with hand built inputs.
	 */
	MatchDto match(Candidate candidate, Set<Integer> heldTraitIds, JobOffer offer) {
		boolean degreeMet = candidate.getDegree() != null
				&& candidate.getDegree().satisfies(offer.getRequiredDegree());

		List<String> missing = offer.getRequirements().stream()
				.filter(OfferRequirement::isMandatory)
				.filter(requirement -> !heldTraitIds.contains(requirement.getTrait().getId()))
				.map(requirement -> requirement.getTrait().getLabel())
				.sorted()
				.toList();

		return new MatchDto(degreeMet && missing.isEmpty(), degreeMet, missing);
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
