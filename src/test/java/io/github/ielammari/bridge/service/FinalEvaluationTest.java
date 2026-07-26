package io.github.ielammari.bridge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import io.github.ielammari.bridge.dto.FinalEvaluationRequest;
import io.github.ielammari.bridge.dto.FinalEvaluationRequest.HiringTerms;
import io.github.ielammari.bridge.dto.FinalEvaluationRequest.InterviewData;
import io.github.ielammari.bridge.dto.OfferDto;
import io.github.ielammari.bridge.dto.OfferRequest;
import io.github.ielammari.bridge.dto.OfferRequest.RequirementSelection;
import io.github.ielammari.bridge.dto.RegisterRequest;
import io.github.ielammari.bridge.dto.TechnicalEvaluationRequest;
import io.github.ielammari.bridge.dto.TechnicalEvaluationRequest.Score;
import io.github.ielammari.bridge.dto.UpdateProfileRequest;
import io.github.ielammari.bridge.dto.UpdateProfileRequest.TraitSelection;
import io.github.ielammari.bridge.exception.ApiException;
import io.github.ielammari.bridge.model.ApplicationStatus;
import io.github.ielammari.bridge.model.ContractType;
import io.github.ielammari.bridge.model.Decision;
import io.github.ielammari.bridge.model.Degree;
import io.github.ielammari.bridge.model.Trait;
import io.github.ielammari.bridge.repository.HiringRepository;
import io.github.ielammari.bridge.repository.TraitRepository;
import io.github.ielammari.bridge.repository.UserRepository;

/** Drives an application to the final HR decision, both outcomes. */
@SpringBootTest
@Transactional
class FinalEvaluationTest {

	@Autowired private AuthService authService;
	@Autowired private ProfileService profileService;
	@Autowired private OfferService offerService;
	@Autowired private ApplicationService applicationService;
	@Autowired private EvaluationService evaluationService;
	@Autowired private TraitRepository traits;
	@Autowired private UserRepository users;
	@Autowired private HiringRepository hirings;

	private Integer hrId() {
		return users.findByEmailIgnoreCase("rh@bridge.local").orElseThrow().getId();
	}

	private Integer expertId() {
		return users.findByEmailIgnoreCase("expert@bridge.local").orElseThrow().getId();
	}

	private Trait aTrait() {
		return traits.findAll().get(0);
	}

	/** Carries an application all the way to the ENTRETIEN_RH stage. */
	private Integer atHrInterview(String email) {
		Integer candidate = authService.register(
				new RegisterRequest(email, "motdepasse1", "Fin", "Test", null)).user().id();
		profileService.update(candidate, new UpdateProfileRequest(Degree.BAC_5, null, null,
				List.of(new TraitSelection(aTrait().getId(), null))));
		profileService.storeCv(candidate,
				new MockMultipartFile("file", "cv.pdf", "application/pdf", "%PDF-1.4".getBytes()));
		OfferDto offer = offerService.create(hrId(), new OfferRequest("Poste", "d", Degree.BAC,
				ContractType.PERMANENT, "Paris", null, null, null,
				List.of(new RequirementSelection(aTrait().getId(), true)), true));
		Integer app = applicationService.apply(candidate, offer.id()).id();
		evaluationService.preselect(hrId(), app, Decision.VALIDEE, null);
		evaluationService.evaluateTechnical(expertId(), app, new TechnicalEvaluationRequest(
				Decision.VALIDEE, "ok", List.of(new Score(aTrait().getId(), (short) 8))));
		return app;
	}

	private InterviewData interview() {
		return new InterviewData(new BigDecimal("45000"), LocalDate.now().plusMonths(2),
				ContractType.PERMANENT, "1 mois", "flexible", null, "Bon fit");
	}

	@Test
	void acceptanceHiresTheCandidateAndCreatesTheHiringRecord() {
		Integer app = atHrInterview("fin1@example.fr");
		HiringTerms terms = new HiringTerms(new BigDecimal("48000"), LocalDate.now().plusMonths(2),
				ContractType.PERMANENT, "3 mois", true, "Tickets restaurant");

		evaluationService.finalize(hrId(), app, new FinalEvaluationRequest(
				Decision.VALIDEE, "Excellent entretien", interview(), terms));

		assertThat(applicationService.forCandidate(
				users.findByEmailIgnoreCase("fin1@example.fr").orElseThrow().getId()))
				.singleElement()
				.satisfies(a -> assertThat(a.status()).isEqualTo(ApplicationStatus.EMBAUCHEE));
		assertThat(hirings.findByApplicationId(app)).isPresent();
	}

	@Test
	void refusalClosesTheApplicationButKeepsTheInterviewData() {
		Integer app = atHrInterview("fin2@example.fr");

		evaluationService.finalize(hrId(), app, new FinalEvaluationRequest(
				Decision.REFUSEE, "Pas retenu", interview(), null));

		assertThat(applicationService.forCandidate(
				users.findByEmailIgnoreCase("fin2@example.fr").orElseThrow().getId()))
				.singleElement()
				.satisfies(a -> assertThat(a.status()).isEqualTo(ApplicationStatus.REFUSEE));
		assertThat(hirings.findByApplicationId(app)).isEmpty();
	}

	@Test
	void acceptanceWithoutHiringTermsIsRejected() {
		Integer app = atHrInterview("fin3@example.fr");

		assertThatThrownBy(() -> evaluationService.finalize(hrId(), app, new FinalEvaluationRequest(
				Decision.VALIDEE, "ok", interview(), null)))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "HIRING_TERMS_REQUIRED");
	}

	@Test
	void finalizingBeforeTheHrInterviewStageIsRejected() {
		Integer candidate = authService.register(
				new RegisterRequest("fin4@example.fr", "motdepasse1", "Too", "Early", null)).user().id();
		profileService.update(candidate, new UpdateProfileRequest(Degree.BAC_5, null, null,
				List.of(new TraitSelection(aTrait().getId(), null))));
		profileService.storeCv(candidate,
				new MockMultipartFile("file", "cv.pdf", "application/pdf", "%PDF-1.4".getBytes()));
		OfferDto offer = offerService.create(hrId(), new OfferRequest("P", "d", Degree.BAC,
				ContractType.PERMANENT, null, null, null, null,
				List.of(new RequirementSelection(aTrait().getId(), true)), true));
		Integer app = applicationService.apply(candidate, offer.id()).id(); // still NOUVELLE

		assertThatThrownBy(() -> evaluationService.finalize(hrId(), app, new FinalEvaluationRequest(
				Decision.REFUSEE, null, interview(), null)))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "INVALID_STATE");
	}

	@Test
	void aSecondFinalizationIsRejected() {
		Integer app = atHrInterview("fin5@example.fr");
		evaluationService.finalize(hrId(), app, new FinalEvaluationRequest(
				Decision.REFUSEE, null, interview(), null));

		assertThatThrownBy(() -> evaluationService.finalize(hrId(), app, new FinalEvaluationRequest(
				Decision.REFUSEE, null, interview(), null)))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "INVALID_STATE");
	}

}
