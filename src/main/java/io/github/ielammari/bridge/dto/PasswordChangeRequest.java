package io.github.ielammari.bridge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Changing a password, proven by the current one. The current password is
 * absent only where there is none to prove, which the service decides.
 */
public record PasswordChangeRequest(

		String currentPassword,

		@NotBlank(message = "Choisissez un nouveau mot de passe.")
		@Size(max = 72, message = "Le mot de passe ne peut pas dépasser 72 caractères.")
		String newPassword) {
}
