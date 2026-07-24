package io.github.ielammari.bridge.dto;

import java.util.List;

import io.github.ielammari.bridge.model.Decision;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * The technical expert's evaluation: a decision, a global comment, and per
 * trait scores. Notes are half star units from 0 to 10 (5 stars, half steps).
 */
public record TechnicalEvaluationRequest(

		@NotNull(message = "La décision est obligatoire.")
		Decision decision,

		@Size(max = 4000, message = "Le commentaire est trop long.")
		String comment,

		@NotEmpty(message = "Notez au moins un trait.")
		@Valid
		List<Score> scores) {

	public record Score(
			@NotNull(message = "Le trait est obligatoire.") Integer traitId,
			@NotNull(message = "La note est obligatoire.")
			@Min(value = 0, message = "La note minimale est 0.")
			@Max(value = 10, message = "La note maximale est 5 étoiles.")
			Short note) {
	}
}
