package io.github.ielammari.bridge.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

		@NotBlank(message = "L'adresse email est obligatoire.")
		String email,

		@NotBlank(message = "Le mot de passe est obligatoire.")
		String password) {
}
