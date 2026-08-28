package io.github.ielammari.bridge.security;

import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import io.github.ielammari.bridge.exception.ApiException;
import io.github.ielammari.bridge.model.User;
import io.github.ielammari.bridge.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * An account can owe a step before the application belongs to it: a password
 * somebody else chose, or details a Google signup could not supply. Nothing
 * else answers until that step is taken. Reading who you are, and taking the
 * step, stay open.
 */
@Component
public class AccountSetupGuard implements HandlerInterceptor {

	private static final Set<String> OPEN = Set.of(
			"/api/v1/auth/me",
			"/api/v1/auth/complete",
			"/api/v1/settings/password");

	private final UserRepository users;

	public AccountSetupGuard(UserRepository users) {
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
		User user = users.findById(id).orElse(null);
		if (user == null) {
			return true;
		}

		if (user.isMustChangePassword()) {
			throw ApiException.forbidden("PASSWORD_CHANGE_REQUIRED",
					"Choisissez un nouveau mot de passe avant d'utiliser ce compte.");
		}
		if (user.mustCompleteProfile()) {
			throw ApiException.forbidden("PROFILE_COMPLETION_REQUIRED",
					"Complétez votre profil avant d'utiliser ce compte.");
		}
		return true;
	}

}
