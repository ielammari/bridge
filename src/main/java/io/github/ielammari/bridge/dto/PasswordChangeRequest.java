package io.github.ielammari.bridge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Changing a password, proven by the current one. */
public record PasswordChangeRequest(

		@NotBlank(message = "Saisissez votre mot de passe actuel.")
		String currentPassword,

		@NotBlank(message = "Choisissez un nouveau mot de passe.")
		@Size(max = 72, message = "Le mot de passe ne peut pas dépasser 72 caractères.")
		String newPassword) {
}
