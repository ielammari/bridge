package io.github.ielammari.bridge.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.ielammari.bridge.dto.MatchDto;
import io.github.ielammari.bridge.dto.OfferDetailDto;
import io.github.ielammari.bridge.dto.OfferDto;
import io.github.ielammari.bridge.dto.OfferMatchDto;
import io.github.ielammari.bridge.dto.OfferRequest;
import io.github.ielammari.bridge.exception.ApiException;
import io.github.ielammari.bridge.mapper.OfferMapper;
import io.github.ielammari.bridge.model.Candidate;
import io.github.ielammari.bridge.model.HRManager;
import io.github.ielammari.bridge.model.JobOffer;
import io.github.ielammari.bridge.model.OfferStatus;
import io.github.ielammari.bridge.model.Role;
import io.github.ielammari.bridge.model.SavedOffer;
import io.github.ielammari.bridge.model.Trait;
import io.github.ielammari.bridge.repository.ApplicationRepository;
import io.github.ielammari.bridge.repository.AppointmentRepository;
import io.github.ielammari.bridge.repository.EvaluationRepository;
import io.github.ielammari.bridge.repository.JobOfferRepository;
import io.github.ielammari.bridge.repository.SavedOfferRepository;
import io.github.ielammari.bridge.repository.TraitRepository;
import io.github.ielammari.bridge.repository.UserRepository;

@Service
public class OfferService {

	private static final String DEFAULT_COMPANY = "Bridge";

	private final JobOfferRepository offers;
	private final TraitRepository traits;
	private final UserRepository users;
	private final ApplicationRepository applications;
	private final AppointmentRepository appointments;
	private final EvaluationRepository evaluations;
	private final SavedOfferRepository saved;
	private final MatchingService matching;

	public OfferService(JobOfferRepository offers, TraitRepository traits, UserRepository users,
			ApplicationRepository applications, AppointmentRepository appointments,
			EvaluationRepository evaluations, SavedOfferRepository saved, MatchingService matching) {
		this.offers = offers;
		this.traits = traits;
		this.users = users;
		this.applications = applications;
		this.appointments = appointments;
		this.evaluations = evaluations;
		this.saved = saved;
		this.matching = matching;
	}

	@Transactional
	public OfferDto create(Integer hrId, OfferRequest request) {
		validate(request);

		JobOffer offer = new JobOffer(requireHr(hrId));
		applyFields(offer, request);
		applyRequirements(offer, request);
		if (request.publishNow()) {
			offer.setStatus(OfferStatus.PUBLIEE);
			offer.setPublicationDate(LocalDate.now());
		}

		return OfferMapper.toDto(offers.save(offer));
	}

	@Transactional
	public OfferDto update(Integer hrId, Integer offerId, OfferRequest request) {
		validate(request);
		JobOffer offer = requireOwnOffer(hrId, offerId);

		applyFields(offer, request);

		// Flush the cleared requirements before re-adding, so the composite keys
		// do not collide with the rows being removed.
		offer.clearRequirements();
		offers.saveAndFlush(offer);
		applyRequirements(offer, request);

		return OfferMapper.toDto(offers.save(offer));
	}

	@Transactional
	public OfferDto publish(Integer hrId, Integer offerId) {
		JobOffer offer = requireOwnOffer(hrId, offerId);
		if (offer.getStatus() == OfferStatus.CLOTUREE) {
			throw ApiException.badRequest("OFFER_CLOSED", "Une offre clôturée ne peut pas être publiée.");
		}
		offer.setStatus(OfferStatus.PUBLIEE);
		offer.setPublicationDate(LocalDate.now());
		return OfferMapper.toDto(offer);
	}

	@Transactional
	public OfferDto close(Integer hrId, Integer offerId) {
		JobOffer offer = requireOwnOffer(hrId, offerId);
		offer.setStatus(OfferStatus.CLOTUREE);
		return OfferMapper.toDto(offer);
	}

	/** The offers this recruiter published, which are the only ones they run. */
	@Transactional(readOnly = true)
	public List<OfferDto> listFor(Integer hrId) {
		return offers.findByPublisherWithRequirements(hrId).stream().map(OfferMapper::toDto).toList();
	}

	@Transactional(readOnly = true)
	public OfferDto get(Integer hrId, Integer offerId) {
		return OfferMapper.toDto(requireOwnOffer(hrId, offerId));
	}

	/**
	 * One offer in full, for whoever is entitled to read it: the recruiter who
	 * published it, a candidate while it is published or once they have applied
	 * to it, and an expert who holds an exam or has assessed someone for it.
	 */
	@Transactional(readOnly = true)
	public OfferDetailDto detail(Integer viewerId, Role viewerRole, Integer offerId) {
		JobOffer offer = requireOffer(offerId);
		boolean applied = applications.existsByCandidateIdAndOfferId(viewerId, offerId);

		boolean allowed = switch (viewerRole) {
			case RH -> offer.getPublisher() != null && offer.getPublisher().getId().equals(viewerId);
			case CANDIDAT -> offer.getStatus() == OfferStatus.PUBLIEE || applied;
			case EXPERT -> evaluations.hasAssessedForOffer(viewerId, offerId)
					|| appointments.existsByApplicationOfferIdAndEvaluatorId(offerId, viewerId);
		};
		if (!allowed) {
			throw ApiException.notFound("Cette offre est introuvable.");
		}

		HRManager publisher = offer.getPublisher();
		MatchDto match = viewerRole == Role.CANDIDAT
				? matching.match(requireCandidate(viewerId), offer)
				: null;

		return new OfferDetailDto(
				OfferMapper.toDto(offer),
				viewerRole == Role.CANDIDAT && saved.existsByCandidateIdAndOfferId(viewerId, offerId),
				publisher == null ? null : publisher.getFirstName() + " " + publisher.getLastName(),
				viewerRole == Role.CANDIDAT && applied,
				viewerRole == Role.RH ? applications.findByOffer(offerId).size() : null,
				match);
	}

	// ---- Saved offers ---------------------------------------------------

	/**
	 * The offers this candidate kept, most recently saved first. A kept offer can
	 * drift out of reach when its traits change, so each carries where the
	 * candidate stands against it.
	 */
	@Transactional(readOnly = true)
	public List<OfferMatchDto> savedFor(Integer candidateId) {
		List<JobOffer> kept = saved.findByCandidateIdOrderBySavedAtDesc(candidateId).stream()
				.map(entry -> offers.findByIdWithRequirements(entry.getOfferId()).orElse(null))
				.filter(java.util.Objects::nonNull)
				.toList();
		return matching.describe(candidateId, kept);
	}

	/**
	 * Keeps or releases an offer, reporting where it ended up. Saving one
	 * already saved is not an error: the button states an intent.
	 */
	@Transactional
	public boolean setSaved(Integer candidateId, Integer offerId, boolean keep) {
		requireOffer(offerId);
		if (keep) {
			if (!saved.existsByCandidateIdAndOfferId(candidateId, offerId)) {
				saved.save(new SavedOffer(candidateId, offerId));
			}
		} else {
			saved.deleteByCandidateIdAndOfferId(candidateId, offerId);
		}
		return keep;
	}

	@Transactional(readOnly = true)
	public boolean isSaved(Integer candidateId, Integer offerId) {
		return saved.existsByCandidateIdAndOfferId(candidateId, offerId);
	}

	private void applyFields(JobOffer offer, OfferRequest request) {
		offer.setTitle(request.title().trim());
		// The deployment's own name stands in when the recruiter leaves it empty.
		String company = blankToNull(request.company());
		offer.setCompany(company == null ? DEFAULT_COMPANY : company);
		offer.setDescription(request.description().trim());
		offer.setRequiredDegree(request.requiredDegree());
		offer.setContractType(request.contractType());
		offer.setLocation(blankToNull(request.location()));
		offer.setRemoteMode(request.remoteMode());
		offer.setSalaryMin(request.salaryMin());
		offer.setSalaryMax(request.salaryMax());
		offer.setWaitForAppointment(request.waitForAppointment());
	}

	private void applyRequirements(JobOffer offer, OfferRequest request) {
		Set<Integer> seen = new LinkedHashSet<>();
		for (OfferRequest.RequirementSelection selection : request.requirements()) {
			if (selection.traitId() == null || !seen.add(selection.traitId())) {
				continue;
			}
			Trait trait = traits.findById(selection.traitId())
					.orElseThrow(() -> ApiException.badRequest("UNKNOWN_TRAIT",
							"Un trait sélectionné n'existe pas."));
			offer.addRequirement(trait, selection.mandatory());
		}
	}

	private void validate(OfferRequest request) {
		BigDecimal min = request.salaryMin();
		BigDecimal max = request.salaryMax();
		if (min != null && max != null && min.compareTo(max) > 0) {
			throw ApiException.badRequest("SALARY_RANGE_INVALID",
					"Le salaire minimum ne peut pas dépasser le salaire maximum.");
		}
		boolean anyMandatory = request.requirements().stream()
				.anyMatch(OfferRequest.RequirementSelection::mandatory);
		if (!anyMandatory) {
			throw ApiException.badRequest("NO_MANDATORY_TRAIT",
					"L'offre doit exiger au moins un trait obligatoire.");
		}
	}

	private Candidate requireCandidate(Integer candidateId) {
		return users.findById(candidateId)
				.filter(Candidate.class::isInstance)
				.map(Candidate.class::cast)
				.orElseThrow(() -> ApiException.notFound("Ce profil candidat est introuvable."));
	}

	private HRManager requireHr(Integer hrId) {
		return users.findById(hrId)
				.filter(HRManager.class::isInstance)
				.map(HRManager.class::cast)
				.orElseThrow(() -> ApiException.notFound("Ce responsable RH est introuvable."));
	}

	private JobOffer requireOffer(Integer offerId) {
		return offers.findByIdWithRequirements(offerId)
				.orElseThrow(() -> ApiException.notFound("Cette offre est introuvable."));
	}

	private JobOffer requireOwnOffer(Integer hrId, Integer offerId) {
		return Ownership.requireOwnOffer(requireOffer(offerId), hrId);
	}

	private String blankToNull(String value) {
		return (value == null || value.isBlank()) ? null : value.trim();
	}

}
