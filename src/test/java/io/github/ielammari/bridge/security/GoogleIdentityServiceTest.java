package io.github.ielammari.bridge.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import io.github.ielammari.bridge.exception.ApiException;
import io.github.ielammari.bridge.security.GoogleIdentityService.GoogleAccount;

/**
 * The claim half of the verification. The signature and expiry are the
 * decoder's, so the token here is built rather than signed.
 */
class GoogleIdentityServiceTest {

	private static final String CLIENT_ID = "bridge.apps.googleusercontent.com";

	private static Jwt.Builder token() {
		return Jwt.withTokenValue("token")
				.header("alg", "RS256")
				.issuedAt(Instant.now())
				.expiresAt(Instant.now().plusSeconds(600))
				.claim("iss", "https://accounts.google.com")
				.claim("aud", List.of(CLIENT_ID))
				.subject("108461234567890123456")
				.claim("email", "Camille.Durand@gmail.com")
				.claim("email_verified", true)
				.claim("given_name", "Camille")
				.claim("family_name", "Durand");
	}

	private static ApiException failure(Jwt.Builder builder) {
		return (ApiException) org.assertj.core.api.Assertions
				.catchThrowable(() -> GoogleIdentityService.read(builder.build(), CLIENT_ID));
	}

	@Test
	void readsTheIdentityFromAValidToken() {
		GoogleAccount account = GoogleIdentityService.read(token().build(), CLIENT_ID);

		assertThat(account.subject()).isEqualTo("108461234567890123456");
		assertThat(account.email()).isEqualTo("camille.durand@gmail.com");
		assertThat(account.firstName()).isEqualTo("Camille");
		assertThat(account.lastName()).isEqualTo("Durand");
	}

	@Test
	void acceptsTheShorterSpellingOfTheIssuer() {
		GoogleAccount account = GoogleIdentityService
				.read(token().claim("iss", "accounts.google.com").build(), CLIENT_ID);

		assertThat(account.subject()).isEqualTo("108461234567890123456");
	}

	@Test
	void refusesAnotherIssuer() {
		assertThat(failure(token().claim("iss", "https://evil.example.com")))
				.hasFieldOrPropertyWithValue("code", "GOOGLE_TOKEN_INVALID");
	}

	@Test
	void refusesATokenMintedForAnotherApplication() {
		assertThat(failure(token().claim("aud", List.of("someone-else.apps.googleusercontent.com"))))
				.hasFieldOrPropertyWithValue("code", "GOOGLE_TOKEN_INVALID");
	}

	@Test
	void refusesAnUnverifiedAddress() {
		assertThat(failure(token().claim("email_verified", false)))
				.hasFieldOrPropertyWithValue("code", "GOOGLE_EMAIL_UNVERIFIED");
	}

	@Test
	void refusesATokenCarryingNoAddress() {
		assertThat(failure(token().claims(claims -> claims.remove("email"))))
				.hasFieldOrPropertyWithValue("code", "GOOGLE_TOKEN_INVALID");
	}

	@Test
	void fallsBackToTheAddressWhenGoogleSendsNoName() {
		GoogleAccount account = GoogleIdentityService.read(
				token().claims((Map<String, Object> claims) -> {
					claims.remove("given_name");
					claims.remove("family_name");
				}).build(),
				CLIENT_ID);

		assertThat(account.firstName()).isEqualTo("Camille.Durand");
		assertThat(account.lastName()).isEqualTo("Camille.Durand");
	}

	@Test
	void refusesToVerifyWhenNoClientIdIsConfigured() {
		assertThatThrownBy(() -> new GoogleIdentityService("").verify("anything"))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "GOOGLE_SIGN_IN_UNAVAILABLE");
	}

}
