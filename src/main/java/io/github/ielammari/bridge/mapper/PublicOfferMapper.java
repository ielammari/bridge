package io.github.ielammari.bridge.mapper;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import io.github.ielammari.bridge.dto.PublicOfferDetailDto;
import io.github.ielammari.bridge.dto.PublicOfferDto;
import io.github.ielammari.bridge.model.JobOffer;
import io.github.ielammari.bridge.model.OfferRequirement;
import io.github.ielammari.bridge.model.Trait;
import io.github.ielammari.bridge.model.TraitCategory;

/** Transformation from an offer entity to what a visitor is shown of it. */
public final class PublicOfferMapper {

	private PublicOfferMapper() {
	}

	public static PublicOfferDto toDto(JobOffer offer) {
		return new PublicOfferDto(
				offer.getId(),
				offer.getTitle(),
				offer.getCompany(),
				offer.getLocation(),
				offer.getContractType(),
				offer.getRemoteMode(),
				offer.getSalaryMin(),
				offer.getSalaryMax(),
				offer.getPublicationDate(),
				domains(List.of(offer)));
	}

	public static PublicOfferDetailDto toDetailDto(JobOffer offer) {
		return new PublicOfferDetailDto(
				toDto(offer),
				offer.getDescription(),
				offer.getRequiredDegree(),
				OfferMapper.requirements(offer));
	}

	/**
	 * The trait categories a set of offers looks for, in the order the pickers
	 * use, so the technical domains come before the ones that qualify a person.
	 */
	public static List<String> domains(Collection<JobOffer> offers) {
		return offers.stream()
				.flatMap(offer -> offer.getRequirements().stream())
				.map(OfferRequirement::getTrait)
				.map(Trait::getCategory)
				.sorted(Comparator.comparing(TraitCategory::getDisplayOrder)
						.thenComparing(TraitCategory::getId))
				.map(TraitCategory::getLabel)
				.distinct()
				.toList();
	}

}
