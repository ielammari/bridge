package io.github.ielammari.bridge.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import io.github.ielammari.bridge.security.PasswordChangeGuard;

/** Applies the account level guards to the API, and to nothing else. */
@Configuration
public class GuardConfig implements WebMvcConfigurer {

	private final PasswordChangeGuard passwordChangeGuard;

	public GuardConfig(PasswordChangeGuard passwordChangeGuard) {
		this.passwordChangeGuard = passwordChangeGuard;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(passwordChangeGuard).addPathPatterns("/api/**");
	}

}
