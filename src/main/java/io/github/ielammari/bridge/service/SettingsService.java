package io.github.ielammari.bridge.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.ielammari.bridge.dto.AccountDto;
import io.github.ielammari.bridge.dto.AccountRequest;
import io.github.ielammari.bridge.dto.NotificationPreferencesDto;
import io.github.ielammari.bridge.dto.OrganisationSettingsDto;
import io.github.ielammari.bridge.dto.PasswordChangeRequest;
import io.github.ielammari.bridge.dto.ProvisionAccountRequest;
import io.github.ielammari.bridge.dto.UserSummary;
import io.github.ielammari.bridge.exception.ApiException;
import io.github.ielammari.bridge.mapper.UserMapper;
import io.github.ielammari.bridge.model.HRManager;
import io.github.ielammari.bridge.model.NotificationPreference;
import io.github.ielammari.bridge.model.NotificationType;
import io.github.ielammari.bridge.model.OrganisationSettings;
import io.github.ielammari.bridge.model.Role;
import io.github.ielammari.bridge.model.TechnicalExpert;
import io.github.ielammari.bridge.model.User;
import io.github.ielammari.bridge.repository.NotificationPreferenceRepository;
import io.github.ielammari.bridge.repository.OrganisationSettingsRepository;
import io.github.ielammari.bridge.repository.UserRepository;

/** Everything an actor can configure, about themselves or about the company. */
@Service
public class SettingsService {

	private final UserRepository users;
	private final NotificationPreferenceRepository preferences;
	private final OrganisationSettingsRepository organisation;
	private final PasswordEncoder passwordEncoder;

	public SettingsService(UserRepository users, NotificationPreferenceRepository preferences,
			OrganisationSettingsRepository organisation, PasswordEncoder passwordEncoder) {
		this.users = users;
		this.preferences = preferences;
		this.organisation = organisation;
		this.passwordEncoder = passwordEncoder;
	}

	// ---- The account ----------------------------------------------------

	@Transactional(readOnly = true)
	public AccountDto account(Integer userId) {
		return toDto(require(userId));
	}

	@Transactional
	public AccountDto updateAccount(Integer userId, AccountRequest request) {
		User user = require(userId);
		String email = request.email().trim().toLowerCase();

		if (!email.equalsIgnoreCase(user.getEmail()) && users.existsByEmailIgnoreCase(email)) {
			throw ApiException.emailAlreadyUsed();
		}

		user.setEmail(email);
		user.setFirstName(request.firstName().trim());
		user.setLastName(request.lastName().trim());
		user.setPhone(blankToNull(request.phone()));
		user.setBirthDate(request.birthDate());
		user.setGender(request.gender());
		user.setCity(blankToNull(request.city()));
		user.setCountry(blankToNull(request.country()));
		return toDto(user);
	}

	/**
	 * The current password is required: without it, an unattended session is
	 * enough to lock the owner out of their own account.
	 */
	@Transactional
	public void changePassword(Integer userId, PasswordChangeRequest request) {
		User user = require(userId);

		if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
			throw ApiException.badRequest("WRONG_PASSWORD", "Le mot de passe actuel est incorrect.");
		}

		PasswordPolicy.check(request.newPassword(), user.getEmail(), user.getFirstName(), user.getLastName());
		user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
	}

	// ---- Notifications --------------------------------------------------

	@Transactional(readOnly = true)
	public NotificationPreferencesDto notifications(Integer userId) {
		Role role = require(userId).getRole();
		return new NotificationPreferencesDto(silenceable(role), always(role),
				preferences.findByUserId(userId).stream().map(NotificationPreference::getType).toList());
	}

	@Transactional
	public NotificationPreferencesDto silence(Integer userId, List<NotificationType> silenced) {
		Role role = require(userId).getRole();
		List<NotificationType> refused = silenced.stream()
				.filter(type -> !type.isSilenceableBy(role)).toList();
		if (!refused.isEmpty()) {
			throw ApiException.badRequest("NOTIFICATION_REQUIRED",
					"Cette notification vous est toujours transmise.");
		}

		preferences.deleteByUserId(userId);
		preferences.flush();
		silenced.stream().distinct()
				.forEach(type -> preferences.save(new NotificationPreference(userId, type)));
		return notifications(userId);
	}

	// ---- The company ----------------------------------------------------

	@Transactional(readOnly = true)
	public OrganisationSettingsDto organisationSettings() {
		OrganisationSettings settings = requireOrganisation();
		return new OrganisationSettingsDto(settings.getFirstHour(), settings.getLastHour());
	}

	@Transactional
	public OrganisationSettingsDto updateOrganisationSettings(OrganisationSettingsDto request) {
		if (request.firstHour() < 0 || request.lastHour() > 23 || request.firstHour() >= request.lastHour()) {
			throw ApiException.badRequest("INVALID_HOURS",
					"La première heure doit précéder la dernière, entre 0h et 23h.");
		}
		requireOrganisation().setHours(request.firstHour(), request.lastHour());
		return organisationSettings();
	}

	/** Creates an HR or expert account, which public signup cannot produce. */
	@Transactional
	public UserSummary provision(ProvisionAccountRequest request) {
		String email = request.email().trim().toLowerCase();
		if (users.existsByEmailIgnoreCase(email)) {
			throw ApiException.emailAlreadyUsed();
		}
		if (request.role() == Role.CANDIDAT) {
			throw ApiException.badRequest("ROLE_NOT_PROVISIONED",
					"Un compte candidat se crée par l'inscription publique.");
		}

		PasswordPolicy.check(request.password(), email, request.firstName(), request.lastName());
		String hash = passwordEncoder.encode(request.password());
		String firstName = request.firstName().trim();
		String lastName = request.lastName().trim();

		User account = request.role() == Role.RH
				? new HRManager(email, hash, firstName, lastName, null)
				: new TechnicalExpert(email, hash, firstName, lastName, null);

		return UserMapper.toSummary(users.save(account));
	}

	private List<NotificationType> silenceable(Role role) {
		return Arrays.stream(NotificationType.values())
				.filter(type -> type.isSilenceableBy(role)).toList();
	}

	/** Received by this role, but not theirs to turn off. */
	private List<NotificationType> always(Role role) {
		return Arrays.stream(NotificationType.values())
				.filter(type -> type.isSentTo(role) && !type.isSilenceableBy(role)).toList();
	}

	private OrganisationSettings requireOrganisation() {
		return organisation.findById(OrganisationSettings.SINGLETON_ID)
				.orElseThrow(() -> ApiException.internal("SETTINGS_MISSING",
						"Les paramètres de l'organisation sont introuvables."));
	}

	private User require(Integer userId) {
		return users.findById(userId)
				.orElseThrow(() -> ApiException.notFound("Ce compte est introuvable."));
	}

	private AccountDto toDto(User user) {
		return new AccountDto(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
				user.getPhone(), user.getBirthDate(), user.getGender(), user.getCity(), user.getCountry(),
				user.getRole(), user.getRegistrationDate());
	}

	private String blankToNull(String value) {
		return (value == null || value.isBlank()) ? null : value.trim();
	}

}
