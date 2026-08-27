package io.github.ielammari.bridge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import io.github.ielammari.bridge.dto.AccountDto;
import io.github.ielammari.bridge.dto.AccountRequest;
import io.github.ielammari.bridge.dto.LoginRequest;
import io.github.ielammari.bridge.dto.OrganisationSettingsDto;
import io.github.ielammari.bridge.dto.PasswordChangeRequest;
import io.github.ielammari.bridge.dto.ProvisionAccountRequest;
import io.github.ielammari.bridge.dto.RegisterRequest;
import io.github.ielammari.bridge.dto.UserSummary;
import io.github.ielammari.bridge.exception.ApiException;
import io.github.ielammari.bridge.model.Gender;
import io.github.ielammari.bridge.model.NotificationType;
import io.github.ielammari.bridge.model.Role;

@SpringBootTest
@Transactional
class SettingsServiceTest {

	@Autowired private AuthService authService;
	@Autowired private SettingsService settingsService;

	private Integer account(String email) {
		return authService.register(new RegisterRequest(email, "Motdepasse1!x", "Set", "Test", null,
				LocalDate.of(1995, 5, 20), null, null, null)).user().id();
	}

	private Integer provisioned(String email, Role role) {
		return settingsService.provision(
				new ProvisionAccountRequest(email, "Set", "Test", "Motdepasse1!x", role)).id();
	}

	private AccountRequest edit(String email) {
		return new AccountRequest(email, "Set", "Test", "0612345678",
				LocalDate.of(1995, 5, 20), Gender.AUTRE, "Lyon", "France");
	}

	// ---- The account ----------------------------------------------------

	@Test
	void theAccountCarriesEveryDetailItsOwnerCanChange() {
		Integer id = account("s1@example.fr");

		AccountDto updated = settingsService.updateAccount(id, edit("s1@example.fr"));

		assertThat(updated.city()).isEqualTo("Lyon");
		assertThat(updated.country()).isEqualTo("France");
		assertThat(updated.gender()).isEqualTo(Gender.AUTRE);
		assertThat(updated.phone()).isEqualTo("0612345678");
	}

	@Test
	void changingTheEmailIsRefusedWhenAnotherAccountHasIt() {
		account("s2a@example.fr");
		Integer second = account("s2b@example.fr");

		assertThatThrownBy(() -> settingsService.updateAccount(second, edit("s2a@example.fr")))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "EMAIL_ALREADY_USED");
	}

	@Test
	void keepingYourOwnEmailIsNotACollision() {
		Integer id = account("s3@example.fr");

		assertThatCode(() -> settingsService.updateAccount(id, edit("s3@example.fr")))
				.doesNotThrowAnyException();
	}

	// ---- The password ---------------------------------------------------

	@Test
	void theNewPasswordTakesEffectAndTheOldOneStopsWorking() {
		Integer id = account("s4@example.fr");

		settingsService.changePassword(id, new PasswordChangeRequest("Motdepasse1!x", "Tour-Eiffel-92"));

		assertThatCode(() -> authService.login(new LoginRequest("s4@example.fr", "Tour-Eiffel-92")))
				.doesNotThrowAnyException();
		assertThatThrownBy(() -> authService.login(new LoginRequest("s4@example.fr", "Motdepasse1!x")))
				.isInstanceOf(ApiException.class);
	}

	/** Without this, an unattended session is enough to take over an account. */
	@Test
	void changingThePasswordDemandsTheCurrentOne() {
		Integer id = account("s5@example.fr");

		assertThatThrownBy(() -> settingsService.changePassword(id,
				new PasswordChangeRequest("pas-le-bon", "Tour-Eiffel-92")))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "WRONG_PASSWORD");
	}

	/** Reusing the old one leaves the account exactly where it was. */
	@Test
	void theNewPasswordMustDifferFromTheOldOne() {
		Integer id = account("s7@example.fr");

		assertThatThrownBy(() -> settingsService.changePassword(id,
				new PasswordChangeRequest("Motdepasse1!x", "Motdepasse1!x")))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "PASSWORD_UNCHANGED");
	}

	@Test
	void aProvisionedAccountIsAskedToChooseItsOwnPassword() {
		settingsService.provision(new ProvisionAccountRequest(
				"expert.neuf2@example.fr", "Nouvel", "Expert", "Tour-Eiffel-92", Role.EXPERT));

		UserSummary signedIn = authService
				.login(new LoginRequest("expert.neuf2@example.fr", "Tour-Eiffel-92")).user();
		assertThat(signedIn.mustChangePassword()).isTrue();

		settingsService.changePassword(signedIn.id(),
				new PasswordChangeRequest("Tour-Eiffel-92", "Pont-Neuf-1607"));

		assertThat(authService.login(new LoginRequest("expert.neuf2@example.fr", "Pont-Neuf-1607"))
				.user().mustChangePassword()).isFalse();
	}

	/** Somebody who chose their own password at signup is never asked again. */
	@Test
	void aSelfRegisteredAccountIsNotAsked() {
		Integer id = account("s8@example.fr");

		assertThat(authService.login(new LoginRequest("s8@example.fr", "Motdepasse1!x"))
				.user().mustChangePassword()).isFalse();
		assertThat(id).isNotNull();
	}

	@Test
	void theNewPasswordMustMeetThePolicy() {
		Integer id = account("s6@example.fr");

		assertThatThrownBy(() -> settingsService.changePassword(id,
				new PasswordChangeRequest("Motdepasse1!x", "court")))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "WEAK_PASSWORD");
	}

	// ---- Notifications --------------------------------------------------

	@Test
	void silencingANotificationIsRemembered() {
		Integer id = provisioned("rh.pref@example.fr", Role.RH);

		settingsService.silence(id, List.of(NotificationType.APPLICATION_RECEIVED));

		assertThat(settingsService.notifications(id).silenced())
				.containsExactly(NotificationType.APPLICATION_RECEIVED);
	}

	/** The list offers what the role receives, and nothing else. */
	@Test
	void thePreferencesAreScopedToTheRole() {
		Integer hr = provisioned("rh.scope@example.fr", Role.RH);
		Integer expert = provisioned("expert.scope@example.fr", Role.EXPERT);
		Integer candidate = account("s7@example.fr");

		assertThat(settingsService.notifications(hr).silenceable())
				.containsExactly(NotificationType.APPLICATION_RECEIVED, NotificationType.SCHEDULE_NEEDED,
						NotificationType.EXAM_OVERDUE);
		// An exam is addressed to one expert, so nothing they receive is optional.
		assertThat(settingsService.notifications(expert).silenceable()).isEmpty();
		assertThat(settingsService.notifications(candidate).silenceable())
				.containsExactly(NotificationType.APPLICATION_SUBMITTED);
	}

	/**
	 * An interview names who must be there, so neither the candidate expected at
	 * it nor the expert holding it can turn it off.
	 */
	@Test
	void anInterviewNoticeCannotBeSilenced() {
		Integer candidate = account("s9@example.fr");
		Integer expert = provisioned("expert.mute@example.fr", Role.EXPERT);

		assertThatThrownBy(() -> settingsService.silence(candidate,
				List.of(NotificationType.INTERVIEW_SCHEDULED)))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "NOTIFICATION_REQUIRED");

		assertThat(settingsService.notifications(candidate).always())
				.containsExactly(NotificationType.INTERVIEW_SCHEDULED,
						NotificationType.REJECTED, NotificationType.HIRED);

		assertThatThrownBy(() -> settingsService.silence(expert,
				List.of(NotificationType.INTERVIEW_SCHEDULED)))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "NOTIFICATION_REQUIRED");

		assertThat(settingsService.notifications(expert).always())
				.containsExactly(NotificationType.INTERVIEW_SCHEDULED, NotificationType.EXAM_UNASSIGNED);
	}

	/** A decision about someone is not a prompt they can opt out of. */
	@Test
	void anOutcomeCannotBeSilenced() {
		Integer id = account("s8@example.fr");

		assertThatThrownBy(() -> settingsService.silence(id, List.of(NotificationType.HIRED)))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "NOTIFICATION_REQUIRED");

		assertThat(settingsService.notifications(id).always())
				.contains(NotificationType.HIRED, NotificationType.REJECTED);
	}

	// ---- The company ----------------------------------------------------

	@Test
	void theInterviewGridIsConfigurable() {
		OrganisationSettingsDto result =
				settingsService.updateOrganisationSettings(new OrganisationSettingsDto((short) 8, (short) 18));

		assertThat(result.firstHour()).isEqualTo((short) 8);
		assertThat(result.lastHour()).isEqualTo((short) 18);
		assertThat(settingsService.organisationSettings().lastHour()).isEqualTo((short) 18);
	}

	@Test
	void aGridThatEndsBeforeItStartsIsRefused() {
		assertThatThrownBy(() -> settingsService
				.updateOrganisationSettings(new OrganisationSettingsDto((short) 18, (short) 8)))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "INVALID_HOURS");
	}

	// ---- Provisioning ---------------------------------------------------

	@Test
	void hrCanProvisionAnExpertAccountThatCanThenSignIn() {
		settingsService.provision(new ProvisionAccountRequest(
				"expert.neuf@example.fr", "Nouvel", "Expert", "Tour-Eiffel-92", Role.EXPERT));

		assertThat(authService.login(new LoginRequest("expert.neuf@example.fr", "Tour-Eiffel-92"))
				.user().role()).isEqualTo(Role.EXPERT);
	}

	/** A candidate account is what public signup is for. */
	@Test
	void provisioningACandidateIsRefused() {
		assertThatThrownBy(() -> settingsService.provision(new ProvisionAccountRequest(
				"cand.neuf@example.fr", "Nouveau", "Candidat", "Tour-Eiffel-92", Role.CANDIDAT)))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "ROLE_NOT_PROVISIONED");
	}

	@Test
	void aProvisionedAccountMustMeetThePasswordPolicy() {
		assertThatThrownBy(() -> settingsService.provision(new ProvisionAccountRequest(
				"faible@example.fr", "Mot", "Faible", "court", Role.RH)))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "WEAK_PASSWORD");
	}

}
