package io.github.ielammari.bridge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import io.github.ielammari.bridge.dto.AuthResponse;
import io.github.ielammari.bridge.dto.GoogleSignInRequest;
import io.github.ielammari.bridge.dto.LoginRequest;
import io.github.ielammari.bridge.dto.ProfileCompletionRequest;
import io.github.ielammari.bridge.dto.RegisterRequest;
import io.github.ielammari.bridge.dto.UserSummary;
import io.github.ielammari.bridge.exception.ApiException;
import io.github.ielammari.bridge.model.Gender;
import io.github.ielammari.bridge.model.Role;
import io.github.ielammari.bridge.repository.UserRepository;
import io.github.ielammari.bridge.security.GoogleIdentityService;
import io.github.ielammari.bridge.security.GoogleIdentityService.GoogleAccount;

@SpringBootTest
@Transactional
class GoogleSignInTest {

	private static final String SUBJECT = "108461234567890123456";

	@Autowired
	private AuthService authService;

	@Autowired
	private UserRepository users;

	@MockitoBean
	private GoogleIdentityService google;

	private AuthResponse signIn(String subject, String email) {
		given(google.verify("token")).willReturn(new GoogleAccount(subject, email, "Camille", "Durand"));
		return authService.signInWithGoogle(new GoogleSignInRequest("token"));
	}

	private static ProfileCompletionRequest completion(LocalDate birthDate) {
		return new ProfileCompletionRequest("Camille", "Durand", "0612345678", birthDate,
				Gender.FEMME, "Rabat", "Maroc");
	}

	@Test
	void aNewSubjectCreatesACandidateThatStillOwesItsProfile() {
		AuthResponse response = signIn(SUBJECT, "google.new@example.fr");

		assertThat(response.user().role()).isEqualTo(Role.CANDIDAT);
		assertThat(response.user().mustCompleteProfile()).isTrue();
		assertThat(response.user().googleLinked()).isTrue();
		assertThat(response.user().hasPassword()).isFalse();
		assertThat(response.token()).isNotBlank();
	}

	@Test
	void theSameSubjectSignsBackIntoTheSameAccount() {
		Integer first = signIn(SUBJECT, "google.repeat@example.fr").user().id();
		Integer second = signIn(SUBJECT, "google.repeat@example.fr").user().id();

		assertThat(second).isEqualTo(first);
		assertThat(users.count()).isNotNull();
	}

	@Test
	void anAddressAlreadyHeldByAPasswordAccountIsRefused() {
		authService.register(new RegisterRequest("google.taken@example.fr", "Motdepasse1!x", "Camille",
				"Durand", "0612345678", LocalDate.of(1995, 5, 20), Gender.FEMME, "Rabat", "Maroc"));

		assertThatThrownBy(() -> signIn(SUBJECT, "google.taken@example.fr"))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "GOOGLE_ACCOUNT_NOT_LINKED");
	}

	@Test
	void aGoogleAccountCannotSignInWithAPassword() {
		signIn(SUBJECT, "google.nopassword@example.fr");

		assertThatThrownBy(() -> authService.login(new LoginRequest("google.nopassword@example.fr", "Motdepasse1!x")))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "GOOGLE_ACCOUNT_ONLY");
	}

	@Test
	void completingTheProfileSettlesWhatTheAccountOwes() {
		Integer id = signIn(SUBJECT, "google.complete@example.fr").user().id();

		UserSummary summary = authService.completeProfile(id, completion(LocalDate.of(1995, 5, 20)));

		assertThat(summary.mustCompleteProfile()).isFalse();
		assertThat(users.findById(id).orElseThrow().getBirthDate()).isEqualTo(LocalDate.of(1995, 5, 20));
	}

	@Test
	void completingTheProfileHoldsTheWorkingAge() {
		Integer id = signIn(SUBJECT, "google.young@example.fr").user().id();

		assertThatThrownBy(() -> authService.completeProfile(id, completion(LocalDate.now().minusYears(15))))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "AGE_TOO_LOW");
	}

	@Test
	void aCompleteProfileCannotBeCompletedAgain() {
		Integer id = signIn(SUBJECT, "google.again@example.fr").user().id();
		authService.completeProfile(id, completion(LocalDate.of(1995, 5, 20)));

		assertThatThrownBy(() -> authService.completeProfile(id, completion(LocalDate.of(1990, 1, 1))))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "PROFILE_ALREADY_COMPLETE");
	}

	@Test
	void aPasswordAccountNeverOwesAProfile() {
		AuthResponse response = authService.register(new RegisterRequest("google.local@example.fr",
				"Motdepasse1!x", "Camille", "Durand", "0612345678", LocalDate.of(1995, 5, 20),
				Gender.FEMME, "Rabat", "Maroc"));

		assertThat(response.user().mustCompleteProfile()).isFalse();
		assertThat(response.user().googleLinked()).isFalse();
		assertThat(response.user().hasPassword()).isTrue();
	}

	@Test
	void theClientIdIsReportedOnlyWhenItIsConfigured() {
		given(google.isConfigured()).willReturn(false);
		assertThat(authService.providers().googleClientId()).isNull();

		given(google.isConfigured()).willReturn(true);
		given(google.clientId()).willReturn("bridge.apps.googleusercontent.com");
		assertThat(authService.providers().googleClientId()).isEqualTo("bridge.apps.googleusercontent.com");
	}

}
