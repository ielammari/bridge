package io.github.ielammari.bridge.dto;

import java.util.List;

import io.github.ielammari.bridge.model.Degree;

/**
 * One open position at full length, carrying what decides an application and
 * nothing internal to the funnel: no status, no recruiter, no timing rule.
 */
public record PublicOfferDetailDto(
		PublicOfferDto offer,
		String description,
		Degree requiredDegree,
		List<OfferRequirementDto> requirements) {
}
