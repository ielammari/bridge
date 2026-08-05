package io.github.ielammari.bridge.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Adding or editing one qualification. An absent end year means the candidate
 * is still reading for it, so only the start year is required.
 */
public record EducationRequest(

		@NotBlank(message = "L'intitulé du diplôme est obligatoire.")
		@Size(max = 150, message = "L'intitulé ne peut pas dépasser 150 caractères.")
		String title,

		@NotBlank(message = "L'établissement est obligatoire.")
		@Size(max = 150, message = "L'établissement ne peut pas dépasser 150 caractères.")
		String institution,

		@Size(max = 150, message = "Le domaine ne peut pas dépasser 150 caractères.")
		String fieldOfStudy,

		@NotNull(message = "L'année de début est obligatoire.")
		@Min(value = 1950, message = "L'année de début doit être postérieure à 1950.")
		@Max(value = 2100, message = "Cette année de début n'est pas valide.")
		Short startYear,

		@Min(value = 1950, message = "L'année de fin doit être postérieure à 1950.")
		@Max(value = 2100, message = "Cette année de fin n'est pas valide.")
		Short endYear) {
}
