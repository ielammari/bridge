package io.github.ielammari.bridge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import io.github.ielammari.bridge.dto.CandidateProfileDto;
import io.github.ielammari.bridge.dto.RegisterRequest;
import io.github.ielammari.bridge.dto.UpdateProfileRequest;
import io.github.ielammari.bridge.dto.UpdateProfileRequest.TraitSelection;
import io.github.ielammari.bridge.exception.ApiException;
import io.github.ielammari.bridge.model.Degree;
import io.github.ielammari.bridge.repository.TraitRepository;

@SpringBootTest
@Transactional
class ProfileServiceTest {

	@Autowired
	private AuthService authService;

	@Autowired
	private ProfileService profileService;

	@Autowired
	private TraitRepository traits;

	private Integer newCandidate(String email) {
		return authService.register(
				new RegisterRequest(email, "motdepasse1", "Ada", "Lovelace", null)).user().id();
	}

	private Integer anyTraitId() {
		return traits.findAll().get(0).getId();
	}

	@Test
	void updateSetsDegreeAndReplacesTheTraitSet() {
		Integer id = newCandidate("profile1@example.fr");
		Integer traitId = anyTraitId();

		CandidateProfileDto profile = profileService.update(id, new UpdateProfileRequest(
				Degree.BAC_5, "5 ans", null, List.of(new TraitSelection(traitId, "Avancé"))));

		assertThat(profile.degree()).isEqualTo(Degree.BAC_5);
		assertThat(profile.experienceLevel()).isEqualTo("5 ans");
		assertThat(profile.traits()).singleElement()
				.satisfies(t -> {
					assertThat(t.traitId()).isEqualTo(traitId);
					assertThat(t.level()).isEqualTo("Avancé");
				});
	}

	@Test
	void updateReplacesRatherThanAppendsTraits() {
		Integer id = newCandidate("profile2@example.fr");
		List<Integer> ids = traits.findAll().stream().limit(3).map(t -> t.getId()).toList();

		profileService.update(id, new UpdateProfileRequest(Degree.BAC, null, null,
				List.of(new TraitSelection(ids.get(0), null), new TraitSelection(ids.get(1), null))));

		CandidateProfileDto after = profileService.update(id, new UpdateProfileRequest(Degree.BAC, null, null,
				List.of(new TraitSelection(ids.get(2), null))));

		assertThat(after.traits()).singleElement()
				.satisfies(t -> assertThat(t.traitId()).isEqualTo(ids.get(2)));
	}

	@Test
	void duplicateTraitSelectionsCollapseToOne() {
		Integer id = newCandidate("profile3@example.fr");
		Integer traitId = anyTraitId();

		CandidateProfileDto profile = profileService.update(id, new UpdateProfileRequest(
				Degree.BAC, null, null,
				List.of(new TraitSelection(traitId, "A"), new TraitSelection(traitId, "B"))));

		assertThat(profile.traits()).hasSize(1);
	}

	@Test
	void anUnknownTraitIsRejected() {
		Integer id = newCandidate("profile4@example.fr");

		assertThatThrownBy(() -> profileService.update(id, new UpdateProfileRequest(
				Degree.BAC, null, null, List.of(new TraitSelection(999_999, null)))))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "UNKNOWN_TRAIT");
	}

	@Test
	void anEmptyTraitListClearsTheProfileTraits() {
		Integer id = newCandidate("profile5@example.fr");
		Integer traitId = anyTraitId();
		profileService.update(id, new UpdateProfileRequest(Degree.BAC, null, null,
				List.of(new TraitSelection(traitId, null))));

		CandidateProfileDto cleared = profileService.update(id,
				new UpdateProfileRequest(Degree.BAC, null, null, List.of()));

		assertThat(cleared.traits()).isEmpty();
	}

}
