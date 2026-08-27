package io.github.ielammari.bridge.dto;

import java.math.BigDecimal;
import java.util.List;

import io.github.ielammari.bridge.model.ContractType;
import io.github.ielammari.bridge.model.Degree;
import io.github.ielammari.bridge.model.RemoteMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Creates or edits an offer. {@code publishNow} applies only on creation: it
 * decides between saving a draft and publishing immediately. Status changes on
 * an existing offer go through the publish and close endpoints.
 * <p>
 * {@code waitForAppointment} holds the evaluators to the hour booked for an
 * interview, so nothing is recorded before it has taken place.
 */
public record OfferRequest(

		@NotBlank(message = "Le titre est obligatoire.")
		@Size(max = 150, message = "Le titre ne peut pas dépasser 150 caractères.")
		String title,

		@Size(max = 120, message = "Le nom de l'entreprise ne peut pas dépasser 120 caractères.")
		String company,

		@NotBlank(message = "La description est obligatoire.")
		String description,

		@NotNull(message = "Le diplôme requis est obligatoire.")
		Degree requiredDegree,

		@NotNull(message = "Le type de contrat est obligatoire.")
		ContractType contractType,

		@Size(max = 120, message = "La localisation ne peut pas dépasser 120 caractères.")
		String location,

		RemoteMode remoteMode,

		@PositiveOrZero(message = "Le salaire minimum ne peut pas être négatif.")
		BigDecimal salaryMin,

		@PositiveOrZero(message = "Le salaire maximum ne peut pas être négatif.")
		BigDecimal salaryMax,

		boolean waitForAppointment,

		@NotEmpty(message = "Sélectionnez au moins un trait pour l'offre.")
		@Valid
		List<RequirementSelection> requirements,

		boolean publishNow) {

	/** One trait attached to the offer, flagged required or plus. */
	public record RequirementSelection(
			@NotNull(message = "Un trait sélectionné est invalide.") Integer traitId,
			boolean mandatory) {
	}
}
