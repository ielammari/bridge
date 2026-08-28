package io.github.ielammari.bridge.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import io.github.ielammari.bridge.security.AccountSetupGuard;

/** Applies the account level guards to the API, and to nothing else. */
@Configuration
public class GuardConfig implements WebMvcConfigurer {

	private final AccountSetupGuard accountSetupGuard;

	public GuardConfig(AccountSetupGuard accountSetupGuard) {
		this.accountSetupGuard = accountSetupGuard;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		// The public offers are not the account's to owe anything for, so an
		// account still owing a setup step reads them like any visitor.
		registry.addInterceptor(accountSetupGuard)
				.addPathPatterns("/api/**")
				.excludePathPatterns("/api/v1/public/**");
	}

}
