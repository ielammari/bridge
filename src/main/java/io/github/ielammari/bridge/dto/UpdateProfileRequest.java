package io.github.ielammari.bridge.dto;

import java.util.List;

import io.github.ielammari.bridge.model.Degree;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Replaces the editable parts of the candidate profile. The trait list is the
 * complete desired set: whatever it contains becomes the profile.
 */
public record UpdateProfileRequest(

		Degree degree,

		@Size(max = 40, message = "Le niveau d'expérience ne peut pas dépasser 40 caractères.")
		String experienceLevel,

		@Pattern(regexp = "^$|^[0-9+ .-]{6,20}$", message = "Ce numéro de téléphone n'est pas valide.")
		String phone,

		@Valid
		List<TraitSelection> traits) {

	/** One selected trait plus its optional level. */
	public record TraitSelection(
			Integer traitId,
			@Size(max = 30, message = "Le niveau ne peut pas dépasser 30 caractères.") String level) {
	}
}
