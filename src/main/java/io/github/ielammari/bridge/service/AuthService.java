package io.github.ielammari.bridge.service;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.ielammari.bridge.dto.AuthProvidersDto;
import io.github.ielammari.bridge.dto.AuthResponse;
import io.github.ielammari.bridge.dto.GoogleSignInRequest;
import io.github.ielammari.bridge.dto.LoginRequest;
import io.github.ielammari.bridge.dto.ProfileCompletionRequest;
import io.github.ielammari.bridge.dto.RegisterRequest;
import io.github.ielammari.bridge.dto.UserSummary;
import io.github.ielammari.bridge.exception.ApiException;
import io.github.ielammari.bridge.mapper.UserMapper;
import io.github.ielammari.bridge.model.Candidate;
import io.github.ielammari.bridge.model.User;
import io.github.ielammari.bridge.repository.UserRepository;
import io.github.ielammari.bridge.security.GoogleIdentityService;
import io.github.ielammari.bridge.security.GoogleIdentityService.GoogleAccount;
import io.github.ielammari.bridge.security.JwtService;

@Service
public class AuthService {

	private static final int MIN_AGE = 18;
	private static final int MAX_AGE = 100;

	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final GoogleIdentityService google;

	public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService,
			GoogleIdentityService google) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.google = google;
	}

	/** Which sign in methods the auth pages may offer. */
	@Transactional(readOnly = true)
	public AuthProvidersDto providers() {
		return new AuthProvidersDto(google.isConfigured() ? google.clientId() : null);
	}

	/** Public signup. Creates a candidate; HR and expert accounts are provisioned separately. */
	@Transactional
	public AuthResponse register(RegisterRequest request) {
		String email = normalize(request.email());
		if (users.existsByEmailIgnoreCase(email)) {
			throw ApiException.emailAlreadyUsed();
		}

		PasswordPolicy.check(request.password(), email, request.firstName(), request.lastName());
		checkAge(request.birthDate());

		Candidate candidate = new Candidate(
				email,
				passwordEncoder.encode(request.password()),
				request.firstName().trim(),
				request.lastName().trim(),
				blankToNull(request.phone()),
				request.birthDate(),
				request.gender(),
				blankToNull(request.city()),
				blankToNull(request.country()));

		return respond(users.save(candidate));
	}

	@Transactional(readOnly = true)
	public AuthResponse login(LoginRequest request) {
		User user = users.findByEmailIgnoreCase(normalize(request.email()))
				.orElseThrow(ApiException::invalidCredentials);

		if (user.getPasswordHash() == null) {
			throw ApiException.unauthorized("GOOGLE_ACCOUNT_ONLY",
					"Ce compte se connecte avec Google.");
		}
		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw ApiException.invalidCredentials();
		}

		return respond(user);
	}

	/**
	 * Signs in the account holding the verified Google subject, or creates a
	 * candidate for a subject and address nobody holds. An address that already
	 * belongs to a password account is refused: linking the two is a deliberate
	 * act, taken from the settings of a session that proved the password.
	 */
	@Transactional
	public AuthResponse signInWithGoogle(GoogleSignInRequest request) {
		GoogleAccount account = google.verify(request.idToken());

		Optional<User> linked = users.findByGoogleSub(account.subject());
		if (linked.isPresent()) {
			return respond(linked.get());
		}

		if (users.existsByEmailIgnoreCase(account.email())) {
			throw ApiException.conflict("GOOGLE_ACCOUNT_NOT_LINKED",
					"Un compte existe déjà avec cette adresse email. Connectez-vous avec votre mot de passe, "
							+ "puis associez Google depuis vos paramètres.");
		}

		Candidate candidate = new Candidate(account.email(), null, account.firstName(), account.lastName(),
				null, null, null, null, null);
		candidate.setGoogleSub(account.subject());

		return respond(users.save(candidate));
	}

	/** The details a Google signup could not supply. */
	@Transactional
	public UserSummary completeProfile(Integer userId, ProfileCompletionRequest request) {
		User user = requireById(userId);

		if (!user.mustCompleteProfile()) {
			throw ApiException.badRequest("PROFILE_ALREADY_COMPLETE", "Ce profil est déjà complet.");
		}

		checkAge(request.birthDate());

		user.setFirstName(request.firstName().trim());
		user.setLastName(request.lastName().trim());
		user.setPhone(blankToNull(request.phone()));
		user.setBirthDate(request.birthDate());
		user.setGender(request.gender());
		user.setCity(blankToNull(request.city()));
		user.setCountry(blankToNull(request.country()));

		return UserMapper.toSummary(user);
	}

	@Transactional(readOnly = true)
	public User requireById(Integer id) {
		return users.findById(id)
				.orElseThrow(() -> ApiException.notFound("Ce compte est introuvable."));
	}

	private AuthResponse respond(User user) {
		JwtService.IssuedToken token = jwtService.issue(user);
		return new AuthResponse(token.value(), token.expiresAt(), UserMapper.toSummary(user));
	}

	/**
	 * The working age range. The upper bound catches a mistyped year, which is
	 * otherwise indistinguishable from a valid date.
	 */
	private void checkAge(LocalDate birthDate) {
		int age = Period.between(birthDate, LocalDate.now()).getYears();
		if (age < MIN_AGE) {
			throw ApiException.badRequest("AGE_TOO_LOW",
					"Vous devez avoir au moins " + MIN_AGE + " ans pour créer un compte.");
		}
		if (age > MAX_AGE) {
			throw ApiException.badRequest("BIRTH_DATE_IMPLAUSIBLE",
					"Vérifiez votre date de naissance, l'année saisie semble incorrecte.");
		}
	}

	private String normalize(String email) {
		return email == null ? null : email.trim().toLowerCase();
	}

	private String blankToNull(String value) {
		return (value == null || value.isBlank()) ? null : value.trim();
	}

}
