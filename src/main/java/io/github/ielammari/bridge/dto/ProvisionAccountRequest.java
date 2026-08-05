package io.github.ielammari.bridge.dto;

import io.github.ielammari.bridge.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Creating an HR or expert account, which nobody can sign up for. */
public record ProvisionAccountRequest(

		@NotBlank(message = "L'adresse email est obligatoire.")
		@Email(message = "Cette adresse email n'est pas valide.")
		@Size(max = 150, message = "L'adresse email ne peut pas dépasser 150 caractères.")
		String email,

		@NotBlank(message = "Le prénom est obligatoire.")
		@Size(max = 80)
		String firstName,

		@NotBlank(message = "Le nom est obligatoire.")
		@Size(max = 80)
		String lastName,

		@NotBlank(message = "Choisissez un mot de passe.")
		@Size(max = 72)
		String password,

		@NotNull(message = "Choisissez un rôle.")
		Role role) {
}
