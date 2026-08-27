package io.github.ielammari.bridge.security;

import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import io.github.ielammari.bridge.exception.ApiException;
import io.github.ielammari.bridge.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * An account created by somebody else starts on a password that person chose,
 * and nothing else in the application answers it until its holder replaces that
 * password. Reading who you are and making the change stay open.
 */
@Component
public class PasswordChangeGuard implements HandlerInterceptor {

	private static final Set<String> OPEN = Set.of(
			"/api/v1/auth/me",
			"/api/v1/settings/password");

	private final UserRepository users;

	public PasswordChangeGuard(UserRepository users) {
		this.users = users;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (OPEN.contains(request.getRequestURI())) {
			return true;
		}

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (!(authentication instanceof JwtAuthenticationToken token)) {
			return true;
		}

		Integer id = Integer.valueOf(token.getToken().getSubject());
		if (users.findById(id).filter(user -> user.isMustChangePassword()).isPresent()) {
			throw ApiException.forbidden("PASSWORD_CHANGE_REQUIRED",
					"Choisissez un nouveau mot de passe avant d'utiliser ce compte.");
		}
		return true;
	}

}
