package io.github.ielammari.bridge.dto;

import java.time.LocalDate;

import io.github.ielammari.bridge.model.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Public signup payload. Creates a candidate account. */
public record RegisterRequest(

		@NotBlank(message = "L'adresse email est obligatoire.")
		@Email(message = "Cette adresse email n'est pas valide.")
		@Size(max = 150, message = "L'adresse email ne peut pas dépasser 150 caractères.")
		String email,

		/* The strength rules live in PasswordPolicy; the cap here is BCrypt's. */
		@NotBlank(message = "Le mot de passe est obligatoire.")
		@Size(max = 72, message = "Le mot de passe ne peut pas dépasser 72 caractères.")
		String password,

		@NotBlank(message = "Le prénom est obligatoire.")
		@Size(max = 80, message = "Le prénom ne peut pas dépasser 80 caractères.")
		String firstName,

		@NotBlank(message = "Le nom est obligatoire.")
		@Size(max = 80, message = "Le nom ne peut pas dépasser 80 caractères.")
		String lastName,

		@Pattern(regexp = "^$|^[0-9+ .-]{6,20}$", message = "Ce numéro de téléphone n'est pas valide.")
		String phone,

		/* The age range itself is checked in AuthService, which can say why. */
		@NotNull(message = "La date de naissance est obligatoire.")
		@Past(message = "La date de naissance doit être dans le passé.")
		LocalDate birthDate,

		Gender gender,

		@Size(max = 100, message = "La ville ne peut pas dépasser 100 caractères.")
		String city,

		@Size(max = 100, message = "Le pays ne peut pas dépasser 100 caractères.")
		String country) {
}
