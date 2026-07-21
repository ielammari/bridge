package io.github.ielammari.bridge.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Public signup payload. Creates a candidate account. */
public record RegisterRequest(

		@NotBlank(message = "L'adresse email est obligatoire.")
		@Email(message = "Cette adresse email n'est pas valide.")
		@Size(max = 150, message = "L'adresse email ne peut pas dépasser 150 caractères.")
		String email,

		@NotBlank(message = "Le mot de passe est obligatoire.")
		@Size(min = 8, max = 72, message = "Le mot de passe doit contenir entre 8 et 72 caractères.")
		String password,

		@NotBlank(message = "Le prénom est obligatoire.")
		@Size(max = 80, message = "Le prénom ne peut pas dépasser 80 caractères.")
		String firstName,

		@NotBlank(message = "Le nom est obligatoire.")
		@Size(max = 80, message = "Le nom ne peut pas dépasser 80 caractères.")
		String lastName,

		@Pattern(regexp = "^$|^[0-9+ .-]{6,20}$", message = "Ce numéro de téléphone n'est pas valide.")
		String phone) {
}
