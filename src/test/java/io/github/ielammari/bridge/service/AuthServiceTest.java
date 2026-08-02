package io.github.ielammari.bridge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import io.github.ielammari.bridge.dto.AuthResponse;
import io.github.ielammari.bridge.dto.LoginRequest;
import io.github.ielammari.bridge.dto.RegisterRequest;
import io.github.ielammari.bridge.exception.ApiException;
import io.github.ielammari.bridge.model.Gender;
import io.github.ielammari.bridge.model.Role;
import io.github.ielammari.bridge.model.User;

@SpringBootTest
@Transactional
class AuthServiceTest {

	@Autowired
	private AuthService authService;

	private static RegisterRequest signup(String email) {
		return signup(email, LocalDate.of(1995, 5, 20));
	}

	private static RegisterRequest signup(String email, LocalDate birthDate) {
		return new RegisterRequest(email, "Motdepasse1!x", "Camille", "Durand", "0612345678",
				birthDate, Gender.FEMME, "Rabat", "Maroc");
	}

	@Test
	void registerCreatesACandidateAndReturnsAToken() {
		AuthResponse response = authService.register(signup("camille@example.fr"));

		assertThat(response.user().role()).isEqualTo(Role.CANDIDAT);
		assertThat(response.user().email()).isEqualTo("camille@example.fr");
		assertThat(response.token()).isNotBlank();
		assertThat(response.expiresAt()).isNotNull();
	}

	@Test
	void registerRejectsAnEmailAlreadyTakenRegardlessOfCase() {
		authService.register(signup("dupe@example.fr"));

		assertThatThrownBy(() -> authService.register(signup("DUPE@example.fr")))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "EMAIL_ALREADY_USED");
	}

	@Test
	void loginAcceptsCorrectCredentials() {
		authService.register(signup("login@example.fr"));

		AuthResponse response = authService.login(new LoginRequest("login@example.fr", "Motdepasse1!x"));

		assertThat(response.user().email()).isEqualTo("login@example.fr");
		assertThat(response.token()).isNotBlank();
	}

	@Test
	void loginRejectsAWrongPassword() {
		authService.register(signup("wrong@example.fr"));

		assertThatThrownBy(() -> authService.login(new LoginRequest("wrong@example.fr", "mauvais-mot-de-passe")))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "INVALID_CREDENTIALS");
	}

	@Test
	void loginRejectsAnUnknownAccountWithTheSameErrorAsAWrongPassword() {
		// Identical failures on both paths, so the response cannot be used to
		// discover which email addresses have accounts.
		assertThatThrownBy(() -> authService.login(new LoginRequest("inconnu@example.fr", "peu-importe")))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "INVALID_CREDENTIALS");
	}

	@Test
	void passwordIsStoredHashedNotInClear() {
		AuthResponse created = authService.register(signup("hash@example.fr"));

		User stored = authService.requireById(created.user().id());

		assertThat(stored.getPasswordHash())
				.startsWith("{bcrypt}$2a$10$")
				.doesNotContain("Motdepasse1!x");
	}

	@Test
	void signupStoresThePersonalDetails() {
		AuthResponse created = authService.register(signup("details@example.fr"));

		User stored = authService.requireById(created.user().id());

		assertThat(stored.getBirthDate()).isEqualTo(LocalDate.of(1995, 5, 20));
		assertThat(stored.getGender()).isEqualTo(Gender.FEMME);
		assertThat(stored.getCity()).isEqualTo("Rabat");
		assertThat(stored.getCountry()).isEqualTo("Maroc");
	}

	@Test
	void signupIsRefusedBelowTheMinimumAge() {
		assertThatThrownBy(() -> authService.register(
				signup("jeune@example.fr", LocalDate.now().minusYears(14))))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "AGE_TOO_LOW");
	}

	/** A mistyped year is otherwise indistinguishable from a valid date. */
	@Test
	void signupIsRefusedForAnImplausibleBirthYear() {
		assertThatThrownBy(() -> authService.register(
				signup("ancien@example.fr", LocalDate.of(1890, 1, 1))))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "BIRTH_DATE_IMPLAUSIBLE");
	}

	@Test
	void anUnansweredGenderIsStoredAsNothingRatherThanAValue() {
		AuthResponse created = authService.register(new RegisterRequest(
				"discret@example.fr", "Motdepasse1!x", "Camille", "Durand", null,
				LocalDate.of(1990, 2, 2), null, null, null));

		assertThat(authService.requireById(created.user().id()).getGender()).isNull();
	}

	@Test
	void everySeededRoleReportsItselfThroughTheBaseType() {
		// The role is answered polymorphically, so no caller needs to test the
		// concrete class to find out what an account is.
		User hr = authService.requireById(authService
				.login(new LoginRequest("rh@bridge.local", "Bridge123!")).user().id());
		User expert = authService.requireById(authService
				.login(new LoginRequest("expert@bridge.local", "Bridge123!")).user().id());

		assertThat(hr.getRole()).isEqualTo(Role.RH);
		assertThat(expert.getRole()).isEqualTo(Role.EXPERT);
	}

}
