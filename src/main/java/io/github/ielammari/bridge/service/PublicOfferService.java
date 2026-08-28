package io.github.ielammari.bridge.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.ielammari.bridge.dto.PublicMarketDto;
import io.github.ielammari.bridge.dto.PublicOfferDetailDto;
import io.github.ielammari.bridge.exception.ApiException;
import io.github.ielammari.bridge.mapper.PublicOfferMapper;
import io.github.ielammari.bridge.model.JobOffer;
import io.github.ielammari.bridge.model.OfferStatus;
import io.github.ielammari.bridge.repository.JobOfferRepository;

/**
 * What the public site reads. Every other offer method answers a caller and
 * decides what that caller may see; here there is no caller, so a published
 * offer is the whole of what exists.
 */
@Service
public class PublicOfferService {

	private final JobOfferRepository offers;

	public PublicOfferService(JobOfferRepository offers) {
		this.offers = offers;
	}

	/** Every open position, most recently published first, and their domains. */
	@Transactional(readOnly = true)
	public PublicMarketDto market() {
		List<JobOffer> open = offers.findByStatusWithRequirements(OfferStatus.PUBLIEE);
		return new PublicMarketDto(
				open.stream().map(PublicOfferMapper::toDto).toList(),
				PublicOfferMapper.domains(open));
	}

	/** One open position in full. A draft and a closed offer are not open. */
	@Transactional(readOnly = true)
	public PublicOfferDetailDto detail(Integer offerId) {
		JobOffer offer = offers.findByIdWithRequirements(offerId)
				.filter(candidate -> candidate.getStatus() == OfferStatus.PUBLIEE)
				.orElseThrow(() -> ApiException.notFound("Cette offre est introuvable."));
		return PublicOfferMapper.toDetailDto(offer);
	}

}
