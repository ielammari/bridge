package io.github.ielammari.bridge.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.github.ielammari.bridge.dto.AccountDto;
import io.github.ielammari.bridge.dto.AccountRequest;
import io.github.ielammari.bridge.dto.GoogleSignInRequest;
import io.github.ielammari.bridge.dto.NotificationPreferencesDto;
import io.github.ielammari.bridge.dto.OrganisationSettingsDto;
import io.github.ielammari.bridge.dto.PasswordChangeRequest;
import io.github.ielammari.bridge.dto.ProvisionAccountRequest;
import io.github.ielammari.bridge.dto.UserSummary;
import io.github.ielammari.bridge.model.NotificationType;
import io.github.ielammari.bridge.service.SettingsService;
import jakarta.validation.Valid;

/** Everything an actor configures. Company wide settings are HR only. */
@RestController
@RequestMapping("/api/v1/settings")
public class SettingsController {

	private final SettingsService settingsService;

	public SettingsController(SettingsService settingsService) {
		this.settingsService = settingsService;
	}

	@GetMapping("/account")
	public AccountDto account(@AuthenticationPrincipal Jwt jwt) {
		return settingsService.account(currentUserId(jwt));
	}

	@PutMapping("/account")
	public AccountDto updateAccount(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AccountRequest request) {
		return settingsService.updateAccount(currentUserId(jwt), request);
	}

	@PostMapping("/password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void changePassword(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody PasswordChangeRequest request) {
		settingsService.changePassword(currentUserId(jwt), request);
	}

	/** Associates a Google identity with the account in session. */
	@PutMapping("/google")
	public UserSummary linkGoogle(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody GoogleSignInRequest request) {
		return settingsService.linkGoogle(Integer.valueOf(jwt.getSubject()), request);
	}

	@DeleteMapping("/google")
	public UserSummary unlinkGoogle(@AuthenticationPrincipal Jwt jwt) {
		return settingsService.unlinkGoogle(Integer.valueOf(jwt.getSubject()));
	}

	@GetMapping("/notifications")
	public NotificationPreferencesDto notifications(@AuthenticationPrincipal Jwt jwt) {
		return settingsService.notifications(currentUserId(jwt));
	}

	@PutMapping("/notifications")
	public NotificationPreferencesDto silence(@AuthenticationPrincipal Jwt jwt,
			@RequestBody List<NotificationType> silenced) {
		return settingsService.silence(currentUserId(jwt), silenced);
	}

	@GetMapping("/organisation")
	public OrganisationSettingsDto organisation() {
		return settingsService.organisationSettings();
	}

	@PutMapping("/organisation")
	public OrganisationSettingsDto updateOrganisation(@RequestBody OrganisationSettingsDto request) {
		return settingsService.updateOrganisationSettings(request);
	}

	@PostMapping("/accounts")
	@ResponseStatus(HttpStatus.CREATED)
	public UserSummary provision(@Valid @RequestBody ProvisionAccountRequest request) {
		return settingsService.provision(request);
	}

	private Integer currentUserId(Jwt jwt) {
		return Integer.valueOf(jwt.getSubject());
	}

}
