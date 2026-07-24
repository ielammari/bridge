package io.github.ielammari.bridge.dto;

import io.github.ielammari.bridge.model.Decision;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** HR's first screening: a decision and a comment. */
public record PreselectionRequest(

		@NotNull(message = "La décision est obligatoire.")
		Decision decision,

		@Size(max = 4000, message = "Le commentaire est trop long.")
		String comment) {
}
