package io.github.ielammari.bridge.mapper;

import java.util.Comparator;
import java.util.List;

import io.github.ielammari.bridge.dto.OfferDto;
import io.github.ielammari.bridge.dto.OfferRequirementDto;
import io.github.ielammari.bridge.model.JobOffer;
import io.github.ielammari.bridge.model.OfferRequirement;
import io.github.ielammari.bridge.model.Trait;

/** Pure transformation from an offer entity to its DTO. */
public final class OfferMapper {

	private OfferMapper() {
	}

	public static OfferDto toDto(JobOffer offer) {
		// Required traits first, then alphabetical, so the view order is stable.
		List<OfferRequirementDto> requirements = offer.getRequirements().stream()
				.sorted(Comparator.comparing(OfferRequirement::isMandatory).reversed()
						.thenComparing(r -> r.getTrait().getLabel()))
				.map(OfferMapper::toRequirementDto)
				.toList();

		return new OfferDto(
				offer.getId(),
				offer.getTitle(),
				offer.getDescription(),
				offer.getRequiredDegree(),
				offer.getContractType(),
				offer.getLocation(),
				offer.getRemoteMode(),
				offer.getSalaryMin(),
				offer.getSalaryMax(),
				offer.getPublicationDate(),
				offer.getStatus(),
				requirements);
	}

	private static OfferRequirementDto toRequirementDto(OfferRequirement requirement) {
		Trait trait = requirement.getTrait();
		return new OfferRequirementDto(
				trait.getId(),
				trait.getLabel(),
				trait.getCategory().getLabel(),
				requirement.isMandatory());
	}

}
