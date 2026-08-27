package io.github.ielammari.bridge.dto;

/** One offer in a candidate listing, with where they stand against it. */
public record OfferMatchDto(
		OfferDto offer,
		MatchDto match) {
}
