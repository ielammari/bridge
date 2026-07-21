package io.github.ielammari.bridge.security;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import io.github.ielammari.bridge.model.User;

/** Issues the access token carrying the user id, role, and email. */
@Service
public class JwtService {

	private final JwtEncoder encoder;
	private final Duration expiry;

	public JwtService(JwtEncoder encoder, @Value("${bridge.jwt.expiry}") Duration expiry) {
		this.encoder = encoder;
		this.expiry = expiry;
	}

	public IssuedToken issue(User user) {
		Instant now = Instant.now();
		Instant expiresAt = now.plus(expiry);

		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer("bridge")
				.issuedAt(now)
				.expiresAt(expiresAt)
				.subject(String.valueOf(user.getId()))
				.claim("role", user.getRole().name())
				.claim("email", user.getEmail())
				.build();

		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		String value = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

		return new IssuedToken(value, expiresAt);
	}

	public record IssuedToken(String value, Instant expiresAt) {
	}

}
