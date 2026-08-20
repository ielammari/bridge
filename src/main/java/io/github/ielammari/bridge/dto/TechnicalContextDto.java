package io.github.ielammari.bridge.dto;

import java.util.List;

/** The context the expert needs to score an application: who, which offer, which traits. */
public record TechnicalContextDto(
		Integer applicationId,
		Integer candidateId,
		String candidateFirstName,
		String candidateLastName,
		Integer offerId,
		String offerTitle,
		List<ExaminedTraitDto> traits) {
}
