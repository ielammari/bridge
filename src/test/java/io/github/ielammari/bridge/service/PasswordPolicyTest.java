package io.github.ielammari.bridge.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import io.github.ielammari.bridge.exception.ApiException;

class PasswordPolicyTest {

	private static final String EMAIL = "camille.durand@example.fr";
	private static final String FIRST = "Camille";
	private static final String LAST = "Durand";

	private void check(String password) {
		PasswordPolicy.check(password, EMAIL, FIRST, LAST);
	}

	@Test
	void aPasswordMeetingEveryRuleIsAccepted() {
		assertThatCode(() -> check("Tour-Eiffel-92")).doesNotThrowAnyException();
	}

	@Test
	void aShortPasswordIsRejected() {
		assertThatThrownBy(() -> check("Court-1!"))
				.isInstanceOf(ApiException.class)
				.hasMessageContaining("12 caractères au minimum");
	}

	@Test
	void aPasswordWithoutAnUppercaseIsRejected() {
		assertThatThrownBy(() -> check("tour-eiffel-92"))
				.isInstanceOf(ApiException.class)
				.hasMessageContaining("une lettre majuscule");
	}

	@Test
	void aPasswordWithoutADigitIsRejected() {
		assertThatThrownBy(() -> check("Tour-Eiffel-Or"))
				.isInstanceOf(ApiException.class)
				.hasMessageContaining("un chiffre");
	}

	@Test
	void aPasswordWithoutASpecialCharacterIsRejected() {
		assertThatThrownBy(() -> check("TourEiffel1992"))
				.isInstanceOf(ApiException.class)
				.hasMessageContaining("un caractère spécial");
	}

	@Test
	void threeIdenticalCharactersInARowAreRejected() {
		assertThatThrownBy(() -> check("Tourrr-Eiffel-92"))
				.isInstanceOf(ApiException.class)
				.hasMessageContaining("trois fois le même caractère");
	}

	@Test
	void aPasswordBuiltFromTheOwnersNameIsRejected() {
		assertThatThrownBy(() -> check("Camille-2024!"))
				.isInstanceOf(ApiException.class)
				.hasMessageContaining("ne reprend ni votre nom");
	}

	/** The name check ignores accents and case, so Elodie does not slip past Élodie. */
	@Test
	void theNameCheckIgnoresAccentsAndCase() {
		assertThatThrownBy(() -> PasswordPolicy.check("elodie-Tour-92!", "e@example.fr", "Élodie", "Martin"))
				.isInstanceOf(ApiException.class)
				.hasMessageContaining("ne reprend ni votre nom");
	}

	/** A fragment under four characters matches too much to be a useful rule. */
	@Test
	void aVeryShortNameIsNotTreatedAsAFragment() {
		assertThatCode(() -> PasswordPolicy.check("Tour-Eiffel-92", "bo@example.fr", "Bo", "Li"))
				.doesNotThrowAnyException();
	}

	@Test
	void everyBrokenRuleIsReportedAtOnce() {
		assertThatThrownBy(() -> check("court"))
				.isInstanceOf(ApiException.class)
				.hasMessageContaining("12 caractères au minimum")
				.hasMessageContaining("une lettre majuscule")
				.hasMessageContaining("un chiffre")
				.hasMessageContaining("un caractère spécial");
	}

}
