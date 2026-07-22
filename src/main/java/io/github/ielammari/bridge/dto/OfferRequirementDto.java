package io.github.ielammari.bridge.dto;

/** A trait an offer looks for, with whether it is required or a plus. */
public record OfferRequirementDto(
		Integer traitId,
		String label,
		String categoryLabel,
		boolean mandatory) {
}
