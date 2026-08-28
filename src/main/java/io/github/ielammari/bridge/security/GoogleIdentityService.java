package io.github.ielammari.bridge.security;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

import io.github.ielammari.bridge.exception.ApiException;

/**
 * Reads the identity a Google ID token asserts, once its signature, issuer,
 * audience and expiry hold. The token is minted by Google and presented by the
 * browser, so nothing in it is trusted before it is verified here.
 */
@Service
public class GoogleIdentityService {

	private static final String JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";

	/** Google mints tokens under either spelling of its issuer. */
	private static final Set<String> ISSUERS = Set.of("https://accounts.google.com", "accounts.google.com");

	private static final int NAME_LIMIT = 80;

	private final String clientId;
	private final JwtDecoder decoder;

	public GoogleIdentityService(@Value("${bridge.oauth.google.client-id:}") String clientId) {
		this.clientId = clientId == null ? "" : clientId.trim();
		// Keys are fetched on first use and cached, so an unconfigured or offline
		// deployment still starts.
		this.decoder = this.clientId.isEmpty() ? null : NimbusJwtDecoder.withJwkSetUri(JWK_SET_URI).build();
	}

	/** The client id the browser needs to render Google's button. */
	public String clientId() {
		return clientId;
	}

	public boolean isConfigured() {
		return decoder != null;
	}

	public GoogleAccount verify(String idToken) {
		if (decoder == null) {
			throw ApiException.badRequest("GOOGLE_SIGN_IN_UNAVAILABLE",
					"La connexion Google n'est pas configurée sur ce serveur.");
		}

		Jwt token;
		try {
			token = decoder.decode(idToken);
		} catch (JwtException failure) {
			throw invalid();
		}

		return read(token, clientId);
	}

	/**
	 * The claim half of the verification, over a token whose signature and
	 * expiry the decoder has already accepted.
	 */
	static GoogleAccount read(Jwt token, String clientId) {
		if (!ISSUERS.contains(token.getClaimAsString("iss"))) {
			throw invalid();
		}

		List<String> audience = token.getAudience();
		if (audience == null || !audience.contains(clientId)) {
			throw invalid();
		}

		// An unverified address would let anyone claim somebody else's account.
		Object verified = token.getClaim("email_verified");
		if (!"true".equalsIgnoreCase(String.valueOf(verified))) {
			throw ApiException.unauthorized("GOOGLE_EMAIL_UNVERIFIED",
					"Cette adresse Google n'est pas vérifiée.");
		}

		String subject = token.getSubject();
		String email = token.getClaimAsString("email");
		if (subject == null || subject.isBlank() || email == null || email.isBlank()) {
			throw invalid();
		}

		String firstName = firstNonBlank(token.getClaimAsString("given_name"),
				token.getClaimAsString("name"), email.split("@")[0]);
		String lastName = firstNonBlank(token.getClaimAsString("family_name"), firstName);

		return new GoogleAccount(subject, email.trim().toLowerCase(), cap(firstName), cap(lastName));
	}

	private static ApiException invalid() {
		return ApiException.unauthorized("GOOGLE_TOKEN_INVALID",
				"La connexion Google a échoué. Réessayez.");
	}

	private static String firstNonBlank(String... values) {
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				return value.trim();
			}
		}
		return "";
	}

	/** The identity columns stop at 80 characters. */
	private static String cap(String value) {
		return value.length() <= NAME_LIMIT ? value : value.substring(0, NAME_LIMIT);
	}

	/** What Google asserts about the person who just signed in. */
	public record GoogleAccount(String subject, String email, String firstName, String lastName) {
	}

}
