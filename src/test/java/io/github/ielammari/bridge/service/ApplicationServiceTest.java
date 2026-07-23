package io.github.ielammari.bridge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import io.github.ielammari.bridge.dto.ApplicationDto;
import io.github.ielammari.bridge.dto.OfferDto;
import io.github.ielammari.bridge.dto.OfferRequest;
import io.github.ielammari.bridge.dto.OfferRequest.RequirementSelection;
import io.github.ielammari.bridge.dto.RegisterRequest;
import io.github.ielammari.bridge.dto.UpdateProfileRequest;
import io.github.ielammari.bridge.dto.UpdateProfileRequest.TraitSelection;
import io.github.ielammari.bridge.exception.ApiException;
import io.github.ielammari.bridge.model.ApplicationStatus;
import io.github.ielammari.bridge.model.ContractType;
import io.github.ielammari.bridge.model.Degree;
import io.github.ielammari.bridge.model.Trait;
import io.github.ielammari.bridge.repository.TraitRepository;
import io.github.ielammari.bridge.repository.UserRepository;

@SpringBootTest
@Transactional
class ApplicationServiceTest {

	@Autowired private AuthService authService;
	@Autowired private ProfileService profileService;
	@Autowired private OfferService offerService;
	@Autowired private ApplicationService applicationService;
	@Autowired private TraitRepository traits;
	@Autowired private UserRepository users;

	private Integer hrId() {
		return users.findByEmailIgnoreCase("rh@bridge.local").orElseThrow().getId();
	}

	private Trait aTrait() {
		return traits.findAll().get(0);
	}

	private Integer candidateWithProfile(String email, boolean withCv) {
		Integer id = authService.register(new RegisterRequest(email, "motdepasse1", "App", "Test", null)).user().id();
		profileService.update(id, new UpdateProfileRequest(Degree.BAC_5, null, null,
				List.of(new TraitSelection(aTrait().getId(), null))));
		if (withCv) {
			profileService.storeCv(id,
					new MockMultipartFile("file", "cv.pdf", "application/pdf", "%PDF-1.4".getBytes()));
		}
		return id;
	}

	private Integer publishedOffer() {
		OfferDto dto = offerService.create(hrId(), new OfferRequest(
				"Poste", "desc", Degree.BAC, ContractType.PERMANENT, "Paris", null, null, null,
				List.of(new RequirementSelection(aTrait().getId(), true)), true));
		return dto.id();
	}

	@Test
	void applyingCreatesANewApplication() {
		Integer candidate = candidateWithProfile("apply1@example.fr", true);
		Integer offer = publishedOffer();

		ApplicationDto dto = applicationService.apply(candidate, offer);

		assertThat(dto.status()).isEqualTo(ApplicationStatus.NOUVELLE);
		assertThat(dto.offerId()).isEqualTo(offer);
	}

	@Test
	void applyingTwiceToTheSameOfferIsRejected() {
		Integer candidate = candidateWithProfile("apply2@example.fr", true);
		Integer offer = publishedOffer();
		applicationService.apply(candidate, offer);

		assertThatThrownBy(() -> applicationService.apply(candidate, offer))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "ALREADY_APPLIED");
	}

	@Test
	void applyingWithoutACvIsRejected() {
		Integer candidate = candidateWithProfile("apply3@example.fr", false); // no CV
		Integer offer = publishedOffer();

		assertThatThrownBy(() -> applicationService.apply(candidate, offer))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "CV_REQUIRED");
	}

	@Test
	void applyingToAnIncompatibleOfferIsRejected() {
		// Candidate holds the trait but only a BAC; the offer demands a doctorate.
		Integer candidate = candidateWithProfile("apply4@example.fr", true);
		profileService.update(candidate, new UpdateProfileRequest(Degree.BAC, null, null,
				List.of(new TraitSelection(aTrait().getId(), null))));
		OfferDto offer = offerService.create(hrId(), new OfferRequest(
				"Docteur", "desc", Degree.DOCTORAT, ContractType.PERMANENT, null, null, null, null,
				List.of(new RequirementSelection(aTrait().getId(), true)), true));

		assertThatThrownBy(() -> applicationService.apply(candidate, offer.id()))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "OFFER_NOT_ACCESSIBLE");
	}

	@Test
	void applyingToADraftOfferIsRejected() {
		Integer candidate = candidateWithProfile("apply5@example.fr", true);
		OfferDto draft = offerService.create(hrId(), new OfferRequest(
				"Brouillon", "desc", Degree.BAC, ContractType.PERMANENT, null, null, null, null,
				List.of(new RequirementSelection(aTrait().getId(), true)), false));

		assertThatThrownBy(() -> applicationService.apply(candidate, draft.id()))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "OFFER_NOT_ACCESSIBLE");
	}

	@Test
	void aCandidateSeesTheirOwnApplications() {
		Integer candidate = candidateWithProfile("apply6@example.fr", true);
		applicationService.apply(candidate, publishedOffer());

		assertThat(applicationService.forCandidate(candidate)).hasSize(1);
	}

	@Test
	void hrSeesTheApplicationsForAnOffer() {
		Integer candidate = candidateWithProfile("apply7@example.fr", true);
		Integer offer = publishedOffer();
		applicationService.apply(candidate, offer);

		assertThat(applicationService.forOffer(offer)).singleElement()
				.satisfies(a -> assertThat(a.candidateEmail()).isEqualTo("apply7@example.fr"));
	}

}
