package io.github.ielammari.bridge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import io.github.ielammari.bridge.dto.ApplicationTrailDto;
import io.github.ielammari.bridge.dto.FinalEvaluationRequest;
import io.github.ielammari.bridge.dto.MyApplicationDetailDto;
import io.github.ielammari.bridge.dto.OfferDto;
import io.github.ielammari.bridge.dto.OfferRequest;
import io.github.ielammari.bridge.dto.OfferRequest.RequirementSelection;
import io.github.ielammari.bridge.dto.RegisterRequest;
import io.github.ielammari.bridge.dto.TechnicalEvaluationRequest;
import io.github.ielammari.bridge.dto.TechnicalEvaluationRequest.Score;
import io.github.ielammari.bridge.dto.UpdateProfileRequest;
import io.github.ielammari.bridge.dto.UpdateProfileRequest.TraitSelection;
import io.github.ielammari.bridge.exception.ApiException;
import io.github.ielammari.bridge.model.ContractType;
import io.github.ielammari.bridge.model.Decision;
import io.github.ielammari.bridge.model.Degree;
import io.github.ielammari.bridge.model.EvaluationType;
import io.github.ielammari.bridge.model.Role;
import io.github.ielammari.bridge.model.Trait;
import io.github.ielammari.bridge.repository.TraitRepository;
import io.github.ielammari.bridge.repository.UserRepository;

/** Reads back the record a completed funnel leaves behind. */
@SpringBootTest
@Transactional
class HistoryServiceTest {

	@Autowired private AuthService authService;
	@Autowired private ProfileService profileService;
	@Autowired private OfferService offerService;
	@Autowired private ApplicationService applicationService;
	@Autowired private EvaluationService evaluationService;
	@Autowired private AppointmentService appointmentService;
	@Autowired private HistoryService historyService;
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
		Integer id = authService.register(new RegisterRequest(email, "Motdepasse1!x", "Hist", "Test", null,
				LocalDate.of(1995, 5, 20), null, null, null)).user().id();
		profileService.update(id, new UpdateProfileRequest(Degree.BAC_5, null,
				List.of(new TraitSelection(aTrait().getId(), null))));
		profileService.storeCv(id, new MockMultipartFile("file", "cv.pdf", "application/pdf", "%PDF-1.4".getBytes()));
		return id;
	}

	private Integer publishedOffer() {
		OfferDto offer = offerService.create(hrId(), new OfferRequest("Poste", "d", Degree.BAC,
				ContractType.PERMANENT, "Paris", null, null, null,
				List.of(new RequirementSelection(aTrait().getId(), true)), true));
		return offer.id();
	}

	/** Carries an application all the way to a hire. */
	private Integer hiredApplication(String email, LocalDate slot) {
		Integer candidateId = candidate(email);
		Integer app = applicationService.apply(candidateId, publishedOffer()).id();
		evaluationService.preselect(hrId(), app, Decision.VALIDEE, "Bon dossier");
		appointmentService.schedule(app, slot, LocalTime.of(10, 0));
		evaluationService.evaluateTechnical(expertId(), app, new TechnicalEvaluationRequest(
				Decision.VALIDEE, "Solide", List.of(new Score(aTrait().getId(), (short) 9))));
		evaluationService.finalize(hrId(), app, new FinalEvaluationRequest(
				Decision.VALIDEE, "Retenu",
				new FinalEvaluationRequest.InterviewData(new BigDecimal("45000"), LocalDate.of(2099, 9, 1),
						ContractType.PERMANENT, "2 mois", "Souple", null, "Bonne"),
				new FinalEvaluationRequest.HiringTerms(new BigDecimal("47000"), LocalDate.of(2099, 10, 1),
						ContractType.PERMANENT, "3 mois", true, "Tickets restaurant")));
		return app;
	}

	@Test
	void theTrailCarriesEveryEvaluationTheFunnelRecorded() {
		Integer app = hiredApplication("h1@example.fr", LocalDate.of(2099, 6, 1));

		ApplicationTrailDto trail = historyService.trail(hrId(), Role.RH, app);

		assertThat(trail.evaluations()).hasSize(3);
		assertThat(trail.evaluations()).extracting(e -> e.type())
				.containsExactly(EvaluationType.PRESELECTION, EvaluationType.TECHNIQUE, EvaluationType.ENTRETIEN_RH);
		assertThat(trail.evaluations().get(0).comment()).isEqualTo("Bon dossier");
	}

	/** The star ratings reach the record, not only the database. */
	@Test
	void theTechnicalEvaluationCarriesItsTraitScores() {
		Integer app = hiredApplication("h2@example.fr", LocalDate.of(2099, 6, 2));

		ApplicationTrailDto trail = historyService.trail(hrId(), Role.RH, app);

		assertThat(trail.evaluations())
				.filteredOn(e -> e.type() == EvaluationType.TECHNIQUE)
				.singleElement()
				.satisfies(e -> {
					assertThat(e.scores()).singleElement()
							.satisfies(s -> assertThat(s.note()).isEqualTo((short) 9));
					assertThat(e.evaluatorName()).isNotBlank();
				});
	}

	@Test
	void theTrailCarriesTheInterviewDataAndTheHiringTerms() {
		Integer app = hiredApplication("h3@example.fr", LocalDate.of(2099, 6, 3));

		ApplicationTrailDto trail = historyService.trail(hrId(), Role.RH, app);

		assertThat(trail.interview()).isNotNull();
		assertThat(trail.interview().noticePeriod()).isEqualTo("2 mois");
		assertThat(trail.hiring()).isNotNull();
		assertThat(trail.hiring().negotiatedSalary()).isEqualByComparingTo("47000");
		assertThat(trail.hiring().benefits()).isEqualTo("Tickets restaurant");
		assertThat(trail.appointments()).isNotEmpty();
	}

	@Test
	void theHiresRegisterListsTheHire() {
		Integer app = hiredApplication("h4@example.fr", LocalDate.of(2099, 6, 4));

		assertThat(historyService.hires())
				.filteredOn(row -> row.applicationId().equals(app))
				.singleElement()
				.satisfies(row -> {
					assertThat(row.candidateEmail()).isEqualTo("h4@example.fr");
					assertThat(row.hiring().finalContract()).isEqualTo(ContractType.PERMANENT);
				});
	}

	@Test
	void anEvaluatorFindsTheEvaluationsTheyWrote() {
		Integer app = hiredApplication("h5@example.fr", LocalDate.of(2099, 6, 5));

		assertThat(historyService.authored(expertId()))
				.filteredOn(row -> row.applicationId().equals(app))
				.singleElement()
				.satisfies(row -> {
					assertThat(row.evaluation().type()).isEqualTo(EvaluationType.TECHNIQUE);
					assertThat(row.offerTitle()).isEqualTo("Poste");
				});
	}

	/** A candidate's own record: the facts and the terms, never an assessment. */
	@Test
	void theCandidateSeesTheirTermsButNoEvaluation() {
		Integer app = hiredApplication("h6@example.fr", LocalDate.of(2099, 6, 6));
		Integer candidateId = users.findByEmailIgnoreCase("h6@example.fr").orElseThrow().getId();

		MyApplicationDetailDto mine = historyService.mine(candidateId, app);

		assertThat(mine.hiring().negotiatedSalary()).isEqualByComparingTo("47000");
		assertThat(mine.appointments()).isNotEmpty();
		// The record carries no field that could hold an evaluation of them.
		assertThat(MyApplicationDetailDto.class.getRecordComponents())
				.extracting(java.lang.reflect.RecordComponent::getName)
				.containsExactly("application", "appointments", "hiring");
	}

	@Test
	void aCandidateCannotReadSomebodyElsesApplication() {
		Integer app = hiredApplication("h7@example.fr", LocalDate.of(2099, 6, 7));
		Integer stranger = candidate("h7b@example.fr");

		assertThatThrownBy(() -> historyService.mine(stranger, app))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "RESOURCE_NOT_FOUND");
	}

	/** An expert reads the trail of an application they were involved in. */
	@Test
	void anExpertReadsTheTrailOfAnApplicationTheyEvaluated() {
		Integer app = hiredApplication("h8@example.fr", LocalDate.of(2099, 6, 8));

		assertThat(historyService.trail(expertId(), Role.EXPERT, app).evaluations()).isNotEmpty();
	}

	@Test
	void anExpertCannotReadTheTrailOfAnApplicationTheyNeverTouched() {
		Integer candidateId = candidate("h9@example.fr");
		Integer app = applicationService.apply(candidateId, publishedOffer()).id();

		assertThatThrownBy(() -> historyService.trail(expertId(), Role.EXPERT, app))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "RESOURCE_NOT_FOUND");
	}

}
