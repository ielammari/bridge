package io.github.ielammari.bridge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import io.github.ielammari.bridge.dto.CandidateProfileDto;
import io.github.ielammari.bridge.dto.EducationDto;
import io.github.ielammari.bridge.dto.EducationRequest;
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
				new RegisterRequest(email, "Motdepasse1!x", "Ada", "Lovelace", null, LocalDate.of(1995, 5, 20), null, null, null)).user().id();
	}

	private Integer anyTraitId() {
		return traits.findAll().get(0).getId();
	}

	@Test
	void updateSetsDegreeAndReplacesTheTraitSet() {
		Integer id = newCandidate("profile1@example.fr");
		Integer traitId = anyTraitId();

		CandidateProfileDto profile = profileService.update(id, new UpdateProfileRequest(
				Degree.BAC_5, null, List.of(new TraitSelection(traitId, "Avancé"))));

		assertThat(profile.degree()).isEqualTo(Degree.BAC_5);
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

		profileService.update(id, new UpdateProfileRequest(Degree.BAC, null,
				List.of(new TraitSelection(ids.get(0), null), new TraitSelection(ids.get(1), null))));

		CandidateProfileDto after = profileService.update(id, new UpdateProfileRequest(Degree.BAC, null,
				List.of(new TraitSelection(ids.get(2), null))));

		assertThat(after.traits()).singleElement()
				.satisfies(t -> assertThat(t.traitId()).isEqualTo(ids.get(2)));
	}

	@Test
	void duplicateTraitSelectionsCollapseToOne() {
		Integer id = newCandidate("profile3@example.fr");
		Integer traitId = anyTraitId();

		CandidateProfileDto profile = profileService.update(id, new UpdateProfileRequest(
				Degree.BAC, null,
				List.of(new TraitSelection(traitId, "A"), new TraitSelection(traitId, "B"))));

		assertThat(profile.traits()).hasSize(1);
	}

	@Test
	void anUnknownTraitIsRejected() {
		Integer id = newCandidate("profile4@example.fr");

		assertThatThrownBy(() -> profileService.update(id, new UpdateProfileRequest(
				Degree.BAC, null, List.of(new TraitSelection(999_999, null)))))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "UNKNOWN_TRAIT");
	}

	@Test
	void anEmptyTraitListClearsTheProfileTraits() {
		Integer id = newCandidate("profile5@example.fr");
		Integer traitId = anyTraitId();
		profileService.update(id, new UpdateProfileRequest(Degree.BAC, null,
				List.of(new TraitSelection(traitId, null))));

		CandidateProfileDto cleared = profileService.update(id,
				new UpdateProfileRequest(Degree.BAC, null, List.of()));

		assertThat(cleared.traits()).isEmpty();
	}

	// ---- The academic path ----------------------------------------------

	@Test
	void theAcademicPathReadsMostRecentFirstWithAnOngoingEntryOnTop() {
		Integer id = newCandidate("path1@example.fr");

		profileService.addEducation(id, new EducationRequest(
				"Licence informatique", "Universite de Lyon", "Informatique",
				(short) 2015, (short) 2018));
		profileService.addEducation(id, new EducationRequest(
				"Master informatique", "INSA Lyon", "Genie logiciel",
				(short) 2018, (short) 2020));
		CandidateProfileDto profile = profileService.addEducation(id, new EducationRequest(
				"Doctorat", "ENS Lyon", null, (short) 2021, null));

		assertThat(profile.education()).extracting(EducationDto::title)
				.containsExactly("Doctorat", "Master informatique", "Licence informatique");
		assertThat(profile.education().get(0).endYear()).isNull();
	}

	/** The path describes the level; it does not become the gate. */
	@Test
	void addingAQualificationLeavesTheScalarLevelAlone() {
		Integer id = newCandidate("path2@example.fr");
		profileService.update(id, new UpdateProfileRequest(Degree.BAC_3, null, List.of()));

		CandidateProfileDto profile = profileService.addEducation(id, new EducationRequest(
				"Master informatique", "INSA Lyon", null, (short) 2018, (short) 2020));

		assertThat(profile.degree()).isEqualTo(Degree.BAC_3);
	}

	@Test
	void aQualificationCannotEndBeforeItBegan() {
		Integer id = newCandidate("path3@example.fr");

		assertThatThrownBy(() -> profileService.addEducation(id, new EducationRequest(
				"Master informatique", "INSA Lyon", null, (short) 2020, (short) 2018)))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "INVALID_PERIOD");
	}

	@Test
	void aQualificationIsEditedAndRemoved() {
		Integer id = newCandidate("path4@example.fr");
		Integer entryId = profileService.addEducation(id, new EducationRequest(
				"Licence", "Universite de Lyon", null, (short) 2015, (short) 2018))
				.education().get(0).id();

		CandidateProfileDto edited = profileService.updateEducation(id, entryId, new EducationRequest(
				"Licence informatique", "Universite Lyon 1", "Informatique",
				(short) 2015, (short) 2018));
		assertThat(edited.education()).singleElement()
				.satisfies(e -> {
					assertThat(e.title()).isEqualTo("Licence informatique");
					assertThat(e.fieldOfStudy()).isEqualTo("Informatique");
				});

		assertThat(profileService.removeEducation(id, entryId).education()).isEmpty();
	}

	/** Someone else's entry is not visible, so it cannot be edited either. */
	@Test
	void aQualificationBelongingToAnotherCandidateCannotBeTouched() {
		Integer mine = newCandidate("path5@example.fr");
		Integer theirs = newCandidate("path6@example.fr");
		Integer entryId = profileService.addEducation(theirs, new EducationRequest(
				"Master", "INSA Lyon", null, (short) 2018, (short) 2020))
				.education().get(0).id();

		assertThatThrownBy(() -> profileService.removeEducation(mine, entryId))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "RESOURCE_NOT_FOUND");
	}

}
