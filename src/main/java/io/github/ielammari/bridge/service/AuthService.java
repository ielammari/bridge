package io.github.ielammari.bridge.service;

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

		Candidate candidate = new Candidate(
				email,
				passwordEncoder.encode(request.password()),
				request.firstName().trim(),
				request.lastName().trim(),
				blankToNull(request.phone()));

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

	private String normalize(String email) {
		return email == null ? null : email.trim().toLowerCase();
	}

	private String blankToNull(String value) {
		return (value == null || value.isBlank()) ? null : value.trim();
	}

}
