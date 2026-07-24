package io.github.ielammari.bridge.dto;

/** A trait presented in the technical scoring grid. */
public record ExaminedTraitDto(
		Integer traitId,
		String label,
		String categoryLabel,
		boolean mandatory) {
}
