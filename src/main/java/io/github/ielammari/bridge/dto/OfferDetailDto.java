package io.github.ielammari.bridge.dto;

/**
 * One offer as a page rather than a card, plus where the reader stands in
 * relation to it. `saved`, `alreadyApplied` and `match` are answered for a
 * candidate and left empty for anyone else; `applicationCount` is answered for
 * the recruiter who runs the offer and null for everyone else.
 */
public record OfferDetailDto(
		OfferDto offer,
		boolean saved,
		String publisherName,
		boolean alreadyApplied,
		Integer applicationCount,
		MatchDto match) {
}
