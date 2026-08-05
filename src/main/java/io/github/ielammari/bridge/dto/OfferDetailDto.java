package io.github.ielammari.bridge.dto;

/**
 * One offer as a page rather than a card: everything a reader is entitled to
 * know about it, plus where they stand in relation to it.
 *
 * `alreadyApplied` is answered for a candidate and false for anyone else;
 * `applicationCount` is answered for the recruiter who runs the offer and null
 * for everyone else, since a candidate has no business knowing how many people
 * they are up against.
 */
public record OfferDetailDto(
		OfferDto offer,
		String publisherName,
		boolean alreadyApplied,
		Integer applicationCount) {
}
