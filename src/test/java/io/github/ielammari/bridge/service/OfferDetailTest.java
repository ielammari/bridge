package io.github.ielammari.bridge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import io.github.ielammari.bridge.dto.OfferDetailDto;
import io.github.ielammari.bridge.dto.OfferDto;
import io.github.ielammari.bridge.dto.OfferRequest;
import io.github.ielammari.bridge.dto.OfferRequest.RequirementSelection;
import io.github.ielammari.bridge.dto.RegisterRequest;
import io.github.ielammari.bridge.dto.UpdateProfileRequest;
import io.github.ielammari.bridge.dto.UpdateProfileRequest.TraitSelection;
import io.github.ielammari.bridge.exception.ApiException;
import io.github.ielammari.bridge.model.ContractType;
import io.github.ielammari.bridge.model.Decision;
import io.github.ielammari.bridge.model.Degree;
import io.github.ielammari.bridge.model.Role;
import io.github.ielammari.bridge.model.Trait;
import io.github.ielammari.bridge.repository.TraitRepository;
import io.github.ielammari.bridge.repository.UserRepository;

/** Who may read one offer in full, and what it tells them. */
@SpringBootTest
@Transactional
class OfferDetailTest {

	@Autowired private AuthService authService;
	@Autowired private ProfileService profileService;
	@Autowired private OfferService offerService;
	@Autowired private ApplicationService applicationService;
	@Autowired private EvaluationService evaluationService;
	@Autowired private TraitRepository traits;
	@Autowired private UserRepository users;

	private Integer hrId() {
		return users.findByEmailIgnoreCase("rh@bridge.local").orElseThrow().getId();
	}

	private Integer expertId() {
		return users.findByEmailIgnoreCase("expert@bridge.local").orElseThrow().getId();
	}

	private Trait aTrait() {
		return traits.findAll().get(0);
	}

	private Integer candidate(String email) {
		Integer id = authService.register(new RegisterRequest(email, "Motdepasse1!x", "Ada", "Lovelace",
				null, LocalDate.of(1995, 5, 20), null, null, null)).user().id();
		profileService.update(id, new UpdateProfileRequest(Degree.BAC_5,
				null, List.of(new TraitSelection(aTrait().getId(), null))));
		profileService.storeCv(id, new MockMultipartFile("file", "cv.pdf", "application/pdf", "%PDF-1.4".getBytes()), null);
		return id;
	}

	private OfferDto offer(boolean published) {
		return offerService.create(hrId(), new OfferRequest("Ingenieur logiciel", null, "Description longue",
				Degree.BAC, ContractType.PERMANENT, "Paris", null, null, null,
				List.of(new RequirementSelection(aTrait().getId(), true)), published));
	}

	@Test
	void theOfferCarriesItsRequirementsAndItsPublisher() {
		OfferDetailDto detail = offerService.detail(hrId(), Role.RH, offer(true).id());

		assertThat(detail.offer().title()).isEqualTo("Ingenieur logiciel");
		assertThat(detail.offer().requirements()).hasSize(1);
		assertThat(detail.publisherName()).isNotBlank();
	}

	/** How many people applied is the recruiter's business, not a reader's. */
	@Test
	void onlyTheRecruiterIsToldHowManyApplied() {
		Integer offerId = offer(true).id();
		Integer id = candidate("od1@example.fr");
		applicationService.apply(id, offerId, null);

		assertThat(offerService.detail(hrId(), Role.RH, offerId).applicationCount()).isEqualTo(1);
		assertThat(offerService.detail(id, Role.CANDIDAT, offerId).applicationCount()).isNull();
	}

	@Test
	void aCandidateIsToldWhetherTheyAlreadyApplied() {
		Integer offerId = offer(true).id();
		Integer applicant = candidate("od2@example.fr");
		Integer other = candidate("od3@example.fr");
		applicationService.apply(applicant, offerId, null);

		assertThat(offerService.detail(applicant, Role.CANDIDAT, offerId).alreadyApplied()).isTrue();
		assertThat(offerService.detail(other, Role.CANDIDAT, offerId).alreadyApplied()).isFalse();
	}

	@Test
	void aDraftIsNotReadableByACandidate() {
		Integer draft = offer(false).id();
		Integer id = candidate("od4@example.fr");

		assertThatThrownBy(() -> offerService.detail(id, Role.CANDIDAT, draft))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "RESOURCE_NOT_FOUND");
	}

	/** Closing an offer must not take its applicants' own subject away. */
	@Test
	void anApplicantStillReadsAnOfferAfterItCloses() {
		Integer offerId = offer(true).id();
		Integer id = candidate("od5@example.fr");
		applicationService.apply(id, offerId, null);
		offerService.close(hrId(), offerId);

		assertThat(offerService.detail(id, Role.CANDIDAT, offerId).offer().id()).isEqualTo(offerId);
	}

	/** A refusal closes an attempt, not the offer. */
	@Test
	void aRefusedCandidateMayApplyAgain() {
		Integer offerId = offer(true).id();
		Integer id = candidate("od6@example.fr");
		Integer first = applicationService.apply(id, offerId, null).id();
		evaluationService.preselect(hrId(), first, Decision.REFUSEE, "Pas cette fois");

		Integer second = applicationService.apply(id, offerId, null).id();

		assertThat(second).isNotEqualTo(first);
	}

	/** Two live applications on one offer stay impossible. */
	@Test
	void aLiveApplicationStillBlocksAnother() {
		Integer offerId = offer(true).id();
		Integer id = candidate("od7@example.fr");
		applicationService.apply(id, offerId, null);

		assertThatThrownBy(() -> applicationService.apply(id, offerId, null))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "ALREADY_APPLIED");
	}

	@Test
	void anOfferIsKeptAndReleased() {
		Integer offerId = offer(true).id();
		Integer id = candidate("od8@example.fr");

		offerService.setSaved(id, offerId, true);
		assertThat(offerService.savedFor(id)).extracting(OfferDto::id).containsExactly(offerId);
		assertThat(offerService.detail(id, Role.CANDIDAT, offerId).saved()).isTrue();

		// Pressing save twice states the same intent, and is not an error.
		offerService.setSaved(id, offerId, true);
		assertThat(offerService.savedFor(id)).hasSize(1);

		offerService.setSaved(id, offerId, false);
		assertThat(offerService.savedFor(id)).isEmpty();
	}

	@Test
	void anExpertReadsOnlyAnOfferTheyHaveWorkedOn() {
		Integer offerId = offer(true).id();

		assertThatThrownBy(() -> offerService.detail(expertId(), Role.EXPERT, offerId))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "RESOURCE_NOT_FOUND");
	}

}
