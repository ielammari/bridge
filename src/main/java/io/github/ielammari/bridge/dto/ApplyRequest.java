package io.github.ielammari.bridge.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Candidate applies to an offer. `cvId` names which of their CVs to attach;
 * without one the profile's current CV is sent.
 */
public record ApplyRequest(
		@NotNull(message = "L'offre est obligatoire.") Integer offerId,
		Integer cvId) {
}
