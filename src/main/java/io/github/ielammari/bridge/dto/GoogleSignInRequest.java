package io.github.ielammari.bridge.dto;

import jakarta.validation.constraints.NotBlank;

/** The ID token Google's button hands to the browser. */
public record GoogleSignInRequest(

		@NotBlank(message = "Le jeton Google est obligatoire.")
		String idToken) {
}
