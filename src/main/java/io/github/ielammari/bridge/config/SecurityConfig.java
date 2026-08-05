package io.github.ielammari.bridge.config;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final String secret;

	public SecurityConfig(@Value("${bridge.jwt.secret}") String secret) {
		this.secret = secret;
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		return http
				// Stateless bearer tokens, so there is no session cookie to protect.
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.GET, "/api/v1/health").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login").permitAll()
						.requestMatchers("/api/v1/profile/**").hasRole("CANDIDAT")
						// The candidate feed is matched before the general offer rules,
						// which are reserved for HR management.
						.requestMatchers(HttpMethod.GET, "/api/v1/offers/feed").hasRole("CANDIDAT")
						// One offer read in full is open to every role, because who may
						// read which offer is a question the service answers, not a
						// question of which role is asking.
						.requestMatchers(HttpMethod.GET, "/api/v1/offers/*/detail").authenticated()
						.requestMatchers("/api/v1/offers", "/api/v1/offers/**").hasRole("RH")
						// Applying and tracking are the candidate's; listing an offer's
						// applications and reading an attached CV are HR's.
						.requestMatchers(HttpMethod.POST, "/api/v1/applications").hasRole("CANDIDAT")
						.requestMatchers(HttpMethod.GET, "/api/v1/applications/mine").hasRole("CANDIDAT")
						.requestMatchers("/api/v1/applications", "/api/v1/applications/**").hasRole("RH")
						// Manual scheduling is HR; the technical evaluation is the expert's.
						.requestMatchers("/api/v1/schedule/**").hasRole("RH")
						.requestMatchers("/api/v1/evaluations/technical/**").hasRole("EXPERT")
						// History: a candidate reads only their own record, the hires
						// register is HR's, and a trail is for whoever ran the funnel.
						.requestMatchers("/api/v1/history/mine/**").hasRole("CANDIDAT")
						.requestMatchers("/api/v1/history/hirings").hasRole("RH")
						.requestMatchers("/api/v1/history/**").hasAnyRole("RH", "EXPERT")
						// Settings: everyone configures their own account, only HR
						// configures the company and provisions other accounts.
						.requestMatchers("/api/v1/settings/organisation", "/api/v1/settings/accounts")
						.hasRole("RH")
						.requestMatchers("/api/v1/settings/**").authenticated()
						.requestMatchers("/api/**").authenticated()
						// Anything else is the SPA shell and its static assets.
						.anyRequest().permitAll())
				.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
				.build();
	}

	/**
	 * Maps the single role claim onto a Spring authority, so a token carrying
	 * "RH" authorizes hasRole("RH").
	 */
	@Bean
	public JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
		authorities.setAuthoritiesClaimName("role");
		authorities.setAuthorityPrefix("ROLE_");

		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(authorities);
		return converter;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		// Stored hashes keep the {bcrypt} prefix, which leaves room to migrate later.
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	public JwtEncoder jwtEncoder() {
		return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey()));
	}

	@Bean
	public JwtDecoder jwtDecoder() {
		return NimbusJwtDecoder.withSecretKey(secretKey())
				.macAlgorithm(MacAlgorithm.HS256)
				.build();
	}

	private SecretKey secretKey() {
		return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

}
