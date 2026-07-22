package io.github.ielammari.bridge.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.ielammari.bridge.dto.OfferDto;
import io.github.ielammari.bridge.dto.OfferRequest;
import io.github.ielammari.bridge.exception.ApiException;
import io.github.ielammari.bridge.mapper.OfferMapper;
import io.github.ielammari.bridge.model.HRManager;
import io.github.ielammari.bridge.model.JobOffer;
import io.github.ielammari.bridge.model.OfferStatus;
import io.github.ielammari.bridge.model.Trait;
import io.github.ielammari.bridge.repository.JobOfferRepository;
import io.github.ielammari.bridge.repository.TraitRepository;
import io.github.ielammari.bridge.repository.UserRepository;

@Service
public class OfferService {

	private final JobOfferRepository offers;
	private final TraitRepository traits;
	private final UserRepository users;

	public OfferService(JobOfferRepository offers, TraitRepository traits, UserRepository users) {
		this.offers = offers;
		this.traits = traits;
		this.users = users;
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
	public OfferDto update(Integer offerId, OfferRequest request) {
		validate(request);
		JobOffer offer = requireOffer(offerId);

		applyFields(offer, request);

		// Flush the cleared requirements before re-adding, so the composite keys
		// do not collide with the rows being removed.
		offer.clearRequirements();
		offers.saveAndFlush(offer);
		applyRequirements(offer, request);

		return OfferMapper.toDto(offers.save(offer));
	}

	@Transactional
	public OfferDto publish(Integer offerId) {
		JobOffer offer = requireOffer(offerId);
		if (offer.getStatus() == OfferStatus.CLOTUREE) {
			throw ApiException.badRequest("OFFER_CLOSED", "Une offre clôturée ne peut pas être publiée.");
		}
		offer.setStatus(OfferStatus.PUBLIEE);
		offer.setPublicationDate(LocalDate.now());
		return OfferMapper.toDto(offer);
	}

	@Transactional
	public OfferDto close(Integer offerId) {
		JobOffer offer = requireOffer(offerId);
		offer.setStatus(OfferStatus.CLOTUREE);
		return OfferMapper.toDto(offer);
	}

	@Transactional(readOnly = true)
	public List<OfferDto> listAll() {
		return offers.findAllWithRequirements().stream().map(OfferMapper::toDto).toList();
	}

	@Transactional(readOnly = true)
	public OfferDto get(Integer offerId) {
		return OfferMapper.toDto(requireOffer(offerId));
	}

	private void applyFields(JobOffer offer, OfferRequest request) {
		offer.setTitle(request.title().trim());
		offer.setDescription(request.description().trim());
		offer.setRequiredDegree(request.requiredDegree());
		offer.setContractType(request.contractType());
		offer.setLocation(blankToNull(request.location()));
		offer.setRemoteMode(request.remoteMode());
		offer.setSalaryMin(request.salaryMin());
		offer.setSalaryMax(request.salaryMax());
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

	private String blankToNull(String value) {
		return (value == null || value.isBlank()) ? null : value.trim();
	}

}
