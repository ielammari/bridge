package io.github.ielammari.bridge.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import io.github.ielammari.bridge.dto.ProvisionAccountRequest;
import io.github.ielammari.bridge.exception.ApiException;
import io.github.ielammari.bridge.model.Role;
import io.github.ielammari.bridge.service.SettingsService;

/**
 * Provisions the first recruiter from the bridge.admin settings when none
 * exists, owing a password change at first sign in. A clean instance with no
 * recruiter and no settings refuses to start.
 */
@Component
@Order(2)
public class AdminAccount implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminAccount.class);

	private final JdbcTemplate jdbc;
	private final SettingsService settings;
	private final boolean demo;
	private final String email;
	private final String password;
	private final String firstName;
	private final String lastName;

	public AdminAccount(JdbcTemplate jdbc, SettingsService settings,
			@Value("${bridge.demo.enabled}") boolean demo,
			@Value("${bridge.admin.email}") String email,
			@Value("${bridge.admin.password}") String password,
			@Value("${bridge.admin.first-name}") String firstName,
			@Value("${bridge.admin.last-name}") String lastName) {
		this.jdbc = jdbc;
		this.settings = settings;
		this.demo = demo;
		this.email = email;
		this.password = password;
		this.firstName = firstName;
		this.lastName = lastName;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM responsable_rh)", Boolean.class)) {
			return;
		}
		if (email.isBlank() || password.isBlank()) {
			if (demo) {
				log.warn("No recruiter account exists; set BRIDGE_ADMIN_EMAIL and BRIDGE_ADMIN_PASSWORD to create one");
				return;
			}
			throw new IllegalStateException("No recruiter account exists. Set BRIDGE_ADMIN_EMAIL and "
					+ "BRIDGE_ADMIN_PASSWORD to create the first one.");
		}
		try {
			settings.provision(new ProvisionAccountRequest(email, firstName, lastName, password, Role.RH));
		} catch (ApiException e) {
			throw new IllegalStateException("The first recruiter account was refused (" + e.getCode() + "): "
					+ e.getMessage(), e);
		}
		log.info("Created the first recruiter account {}; its password must be changed at first sign in", email);
	}

}
