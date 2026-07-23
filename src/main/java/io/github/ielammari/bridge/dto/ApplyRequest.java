package io.github.ielammari.bridge.dto;

import jakarta.validation.constraints.NotNull;

/** Candidate applies to an offer. The CV is taken from the profile. */
public record ApplyRequest(
		@NotNull(message = "L'offre est obligatoire.") Integer offerId) {
}
