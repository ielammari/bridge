package io.github.ielammari.bridge.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.github.ielammari.bridge.dto.AuthResponse;
import io.github.ielammari.bridge.dto.LoginRequest;
import io.github.ielammari.bridge.dto.RegisterRequest;
import io.github.ielammari.bridge.dto.UserSummary;
import io.github.ielammari.bridge.mapper.UserMapper;
import io.github.ielammari.bridge.service.AuthService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
		return authService.register(request);
	}

	@PostMapping("/login")
	public AuthResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}

	/** Returns the account behind the bearer token, used to restore a session on reload. */
	@GetMapping("/me")
	public UserSummary me(@AuthenticationPrincipal Jwt jwt) {
		return UserMapper.toSummary(authService.requireById(Integer.valueOf(jwt.getSubject())));
	}

}
