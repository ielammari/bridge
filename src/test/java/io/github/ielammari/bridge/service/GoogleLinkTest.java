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

import io.github.ielammari.bridge.dto.GoogleSignInRequest;
import io.github.ielammari.bridge.dto.PasswordChangeRequest;
import io.github.ielammari.bridge.dto.RegisterRequest;
import io.github.ielammari.bridge.dto.UserSummary;
import io.github.ielammari.bridge.exception.ApiException;
import io.github.ielammari.bridge.model.Gender;
import io.github.ielammari.bridge.repository.UserRepository;
import io.github.ielammari.bridge.security.GoogleIdentityService;
import io.github.ielammari.bridge.security.GoogleIdentityService.GoogleAccount;

@SpringBootTest
@Transactional
class GoogleLinkTest {

	private static final String SUBJECT = "108461234567890123456";
	private static final String OTHER_SUBJECT = "108461234567890199999";

	@Autowired
	private AuthService authService;

	@Autowired
	private SettingsService settingsService;

	@Autowired
	private UserRepository users;

	@MockitoBean
	private GoogleIdentityService google;

	private Integer candidate(String email) {
		return authService.register(new RegisterRequest(email, "Motdepasse1!x", "Camille", "Durand",
				"0612345678", LocalDate.of(1995, 5, 20), Gender.FEMME, "Rabat", "Maroc")).user().id();
	}

	private UserSummary link(Integer userId, String subject, String email) {
		given(google.verify("token")).willReturn(new GoogleAccount(subject, email, "Camille", "Durand"));
		return settingsService.linkGoogle(userId, new GoogleSignInRequest("token"));
	}

	@Test
	void linkingLetsTheSameSubjectSignBackIn() {
		Integer id = candidate("link.ok@example.fr");

		assertThat(link(id, SUBJECT, "personal@gmail.com").googleLinked()).isTrue();

		given(google.verify("token")).willReturn(new GoogleAccount(SUBJECT, "personal@gmail.com", "Camille", "Durand"));
		assertThat(authService.signInWithGoogle(new GoogleSignInRequest("token")).user().id()).isEqualTo(id);
	}

	@Test
	void theGoogleAddressNeedNotMatchTheAccountAddress() {
		Integer id = candidate("link.work@example.fr");

		link(id, SUBJECT, "quite.different@gmail.com");

		assertThat(users.findById(id).orElseThrow().getEmail()).isEqualTo("link.work@example.fr");
		assertThat(users.findByGoogleSub(SUBJECT).orElseThrow().getId()).isEqualTo(id);
	}

	@Test
	void aSubjectAlreadyHeldElsewhereIsRefused() {
		Integer first = candidate("link.first@example.fr");
		Integer second = candidate("link.second@example.fr");
		link(first, SUBJECT, "personal@gmail.com");

		assertThatThrownBy(() -> link(second, SUBJECT, "personal@gmail.com"))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "GOOGLE_ALREADY_LINKED");
	}

	@Test
	void linkingAgainWithTheSameSubjectIsNotAConflictWithItself() {
		Integer id = candidate("link.again@example.fr");
		link(id, SUBJECT, "personal@gmail.com");

		assertThat(link(id, SUBJECT, "personal@gmail.com").googleLinked()).isTrue();
	}

	@Test
	void anotherSubjectReplacesTheOne() {
		Integer id = candidate("link.swap@example.fr");
		link(id, SUBJECT, "personal@gmail.com");

		link(id, OTHER_SUBJECT, "second@gmail.com");

		assertThat(users.findByGoogleSub(SUBJECT)).isEmpty();
		assertThat(users.findByGoogleSub(OTHER_SUBJECT).orElseThrow().getId()).isEqualTo(id);
	}

	@Test
	void aRecruiterCannotLinkGoogle() {
		Integer rh = users.findByEmailIgnoreCase("rh@bridge.local").orElseThrow().getId();

		assertThatThrownBy(() -> link(rh, SUBJECT, "recruiter@gmail.com"))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "GOOGLE_LINK_NOT_ALLOWED");
	}

	@Test
	void unlinkingNeedsSomethingLinked() {
		Integer id = candidate("unlink.none@example.fr");

		assertThatThrownBy(() -> settingsService.unlinkGoogle(id))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "GOOGLE_NOT_LINKED");
	}

	@Test
	void unlinkingLeavesTheAccountReachable() {
		Integer id = candidate("unlink.ok@example.fr");
		link(id, SUBJECT, "personal@gmail.com");

		assertThat(settingsService.unlinkGoogle(id).googleLinked()).isFalse();
		assertThat(users.findById(id).orElseThrow().getPasswordHash()).isNotNull();
	}

	@Test
	void anAccountWithNoPasswordCannotUnlinkItsOnlyWayIn() {
		given(google.verify("token")).willReturn(new GoogleAccount(SUBJECT, "google.only@gmail.com", "Camille", "Durand"));
		Integer id = authService.signInWithGoogle(new GoogleSignInRequest("token")).user().id();

		assertThatThrownBy(() -> settingsService.unlinkGoogle(id))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "PASSWORD_REQUIRED_FIRST");
	}

	@Test
	void aFirstPasswordIsSetWithoutProvingOne() {
		given(google.verify("token")).willReturn(new GoogleAccount(SUBJECT, "google.first@gmail.com", "Camille", "Durand"));
		Integer id = authService.signInWithGoogle(new GoogleSignInRequest("token")).user().id();

		settingsService.changePassword(id, new PasswordChangeRequest(null, "Motdepasse1!x"));

		assertThat(users.findById(id).orElseThrow().getPasswordHash()).isNotNull();
		assertThat(settingsService.unlinkGoogle(id).googleLinked()).isFalse();
	}

	@Test
	void aFirstPasswordStillObeysThePolicy() {
		given(google.verify("token")).willReturn(new GoogleAccount(SUBJECT, "google.weak@gmail.com", "Camille", "Durand"));
		Integer id = authService.signInWithGoogle(new GoogleSignInRequest("token")).user().id();

		assertThatThrownBy(() -> settingsService.changePassword(id, new PasswordChangeRequest(null, "court")))
				.isInstanceOf(ApiException.class);
	}

	@Test
	void anAccountWithAPasswordStillHasToProveIt() {
		Integer id = candidate("unlink.proof@example.fr");

		assertThatThrownBy(() -> settingsService.changePassword(id, new PasswordChangeRequest(null, "Motdepasse2!y")))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "WRONG_PASSWORD");
	}

}
