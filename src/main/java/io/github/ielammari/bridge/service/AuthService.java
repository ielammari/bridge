package io.github.ielammari.bridge.service;

import java.time.LocalDate;
import java.time.Period;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.ielammari.bridge.dto.AuthResponse;
import io.github.ielammari.bridge.dto.LoginRequest;
import io.github.ielammari.bridge.dto.RegisterRequest;
import io.github.ielammari.bridge.exception.ApiException;
import io.github.ielammari.bridge.mapper.UserMapper;
import io.github.ielammari.bridge.model.Candidate;
import io.github.ielammari.bridge.model.User;
import io.github.ielammari.bridge.repository.UserRepository;
import io.github.ielammari.bridge.security.JwtService;

@Service
public class AuthService {

	private static final int MIN_AGE = 18;
	private static final int MAX_AGE = 100;

	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
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

		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw ApiException.invalidCredentials();
		}

		return respond(user);
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
