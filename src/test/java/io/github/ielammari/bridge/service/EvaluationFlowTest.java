package io.github.ielammari.bridge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import io.github.ielammari.bridge.dto.HrApplicationDto;
import io.github.ielammari.bridge.dto.OfferDto;
import io.github.ielammari.bridge.dto.OfferRequest;
import io.github.ielammari.bridge.dto.OfferRequest.RequirementSelection;
import io.github.ielammari.bridge.dto.RegisterRequest;
import io.github.ielammari.bridge.dto.ProvisionAccountRequest;
import io.github.ielammari.bridge.dto.TechnicalEvaluationRequest;
import io.github.ielammari.bridge.dto.TechnicalEvaluationRequest.Score;
import io.github.ielammari.bridge.dto.UpdateProfileRequest;
import io.github.ielammari.bridge.dto.UpdateProfileRequest.TraitSelection;
import io.github.ielammari.bridge.exception.ApiException;
import io.github.ielammari.bridge.model.ApplicationStatus;
import io.github.ielammari.bridge.model.ContractType;
import io.github.ielammari.bridge.model.Decision;
import io.github.ielammari.bridge.model.Degree;
import io.github.ielammari.bridge.model.Role;
import io.github.ielammari.bridge.model.Trait;
import io.github.ielammari.bridge.repository.TraitRepository;
import io.github.ielammari.bridge.repository.UserRepository;

/** Drives an application through preselection, scheduling, and the technical exam. */
@SpringBootTest
@Transactional
class EvaluationFlowTest {

	@Autowired private AuthService authService;
	@Autowired private ProfileService profileService;
	@Autowired private OfferService offerService;
	@Autowired private ApplicationService applicationService;
	@Autowired private EvaluationService evaluationService;
	@Autowired private AppointmentService appointmentService;
	@Autowired private SettingsService settingsService;
	@Autowired private TraitRepository traits;
	@Autowired private UserRepository users;

	private Integer hrId() {
		return users.findByEmailIgnoreCase("rh@bridge.local").orElseThrow().getId();
	}

	private Integer expertId() {
		return users.findByEmailIgnoreCase("expert@bridge.local").orElseThrow().getId();
	}

	/** Books the exam the technical evaluation requires. */
	private void bookExam(Integer app) {
		appointmentService.schedule(hrId(), app, LocalDate.of(2099, 1, 1).plusDays(app % 1000),
				LocalTime.of(9, 0), expertId());
	}

	private Trait aTrait() {
		return traits.findAll().get(0);
	}

	/** A candidate with a CV and profile who has applied to a fresh published offer. */
	private Integer applyToFreshOffer(String email) {
		Integer candidate = authService.register(
				new RegisterRequest(email, "Motdepasse1!x", "Eval", "Test", null, LocalDate.of(1995, 5, 20), null, null, null)).user().id();
		profileService.update(candidate, new UpdateProfileRequest(Degree.BAC_5, null,
				List.of(new TraitSelection(aTrait().getId(), null))));
		profileService.storeCv(candidate, new MockMultipartFile("file", "cv.pdf", "application/pdf", "%PDF-1.4".getBytes()), null);

		OfferDto offer = offerService.create(hrId(), new OfferRequest("Poste", null, "d", Degree.BAC,
				ContractType.PERMANENT, "Paris", null, null, null,
				false, List.of(new RequirementSelection(aTrait().getId(), true)), true));
		return applicationService.apply(candidate, offer.id(), null).id();
	}

	@Test
	void preselectionApprovalAdvancesToTheTechnicalExam() {
		Integer app = applyToFreshOffer("ev1@example.fr");

		HrApplicationDto result = evaluationService.preselect(hrId(), app, Decision.VALIDEE, "Bon profil");

		assertThat(result.status()).isEqualTo(ApplicationStatus.EXAMEN_TECHNIQUE);
	}

	@Test
	void preselectionRejectionClosesTheApplication() {
		Integer app = applyToFreshOffer("ev2@example.fr");

		HrApplicationDto result = evaluationService.preselect(hrId(), app, Decision.REFUSEE, "Pas retenu");

		assertThat(result.status()).isEqualTo(ApplicationStatus.REFUSEE);
	}

	@Test
	void aSecondPreselectionIsRejected() {
		Integer app = applyToFreshOffer("ev3@example.fr");
		evaluationService.preselect(hrId(), app, Decision.VALIDEE, null);

		assertThatThrownBy(() -> evaluationService.preselect(hrId(), app, Decision.VALIDEE, null))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "INVALID_STATE");
	}

	@Test
	void hrSchedulesTheTechnicalExamOntoAFreeSlot() {
		Integer app = applyToFreshOffer("ev4@example.fr");
		evaluationService.preselect(hrId(), app, Decision.VALIDEE, null);

		// The company wide calendar allows one interview per date and time, so a
		// far future slot keeps this off any appointment already in the database.
		LocalDate date = LocalDate.of(2099, 1, 1);
		HrApplicationDto scheduled = appointmentService.schedule(hrId(), app, date, LocalTime.of(10, 0), expertId());

		assertThat(scheduled.appointmentDate()).isEqualTo(date);
		assertThat(scheduled.appointmentTime()).isEqualTo(LocalTime.of(10, 0));
	}

	@Test
	void aTakenSlotCannotBeReused() {
		Integer app1 = applyToFreshOffer("ev5a@example.fr");
		Integer app2 = applyToFreshOffer("ev5b@example.fr");
		evaluationService.preselect(hrId(), app1, Decision.VALIDEE, null);
		evaluationService.preselect(hrId(), app2, Decision.VALIDEE, null);

		LocalDate date = LocalDate.of(2099, 1, 2);
		appointmentService.schedule(hrId(), app1, date, LocalTime.of(11, 0), expertId());

		assertThatThrownBy(() -> appointmentService.schedule(hrId(), app2, date, LocalTime.of(11, 0), expertId()))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "SLOT_TAKEN");
	}

	@Test
	void schedulingAnApplicationThatIsNotWaitingIsRejected() {
		Integer app = applyToFreshOffer("ev6@example.fr"); // still NOUVELLE

		assertThatThrownBy(() -> appointmentService.schedule(hrId(), app, LocalDate.now().plusDays(1), LocalTime.of(9, 0), expertId()))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "NOTHING_TO_SCHEDULE");
	}

	@Test
	void anHourOutsideTheGridIsRejected() {
		Integer app = applyToFreshOffer("ev7@example.fr");
		evaluationService.preselect(hrId(), app, Decision.VALIDEE, null);

		assertThatThrownBy(() -> appointmentService.schedule(hrId(), app, LocalDate.now().plusDays(1), LocalTime.of(17, 30), expertId()))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "SLOT_INVALID");
	}

	@Test
	void favorableTechnicalEvaluationAdvancesToTheHrInterview() {
		Integer app = applyToFreshOffer("ev8@example.fr");
		evaluationService.preselect(hrId(), app, Decision.VALIDEE, null);

		bookExam(app);
		evaluationService.evaluateTechnical(expertId(), app, new TechnicalEvaluationRequest(
				Decision.VALIDEE, "Solide", List.of(new Score(aTrait().getId(), (short) 8))));

		assertThat(applicationService.forCandidate(
				users.findByEmailIgnoreCase("ev8@example.fr").orElseThrow().getId()))
				.singleElement()
				.satisfies(a -> assertThat(a.status()).isEqualTo(ApplicationStatus.ENTRETIEN_RH));
	}

	@Test
	void unfavorableTechnicalEvaluationClosesTheApplication() {
		Integer app = applyToFreshOffer("ev9@example.fr");
		evaluationService.preselect(hrId(), app, Decision.VALIDEE, null);

		bookExam(app);
		evaluationService.evaluateTechnical(expertId(), app, new TechnicalEvaluationRequest(
				Decision.REFUSEE, "Insuffisant", List.of(new Score(aTrait().getId(), (short) 2))));

		assertThat(applicationService.forCandidate(
				users.findByEmailIgnoreCase("ev9@example.fr").orElseThrow().getId()))
				.singleElement()
				.satisfies(a -> assertThat(a.status()).isEqualTo(ApplicationStatus.REFUSEE));
	}

	@Test
	void scoringATraitNotOnTheOfferIsRejected() {
		Integer app = applyToFreshOffer("ev10@example.fr");
		evaluationService.preselect(hrId(), app, Decision.VALIDEE, null);
		Integer otherTrait = traits.findAll().get(50).getId();

		bookExam(app);
		assertThatThrownBy(() -> evaluationService.evaluateTechnical(expertId(), app,
				new TechnicalEvaluationRequest(Decision.VALIDEE, null,
						List.of(new Score(otherTrait, (short) 5)))))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "NOT_EXAMINED_TRAIT");
	}

	@Test
	void aNoteAboveFiveStarsIsRejected() {
		Integer app = applyToFreshOffer("ev11@example.fr");
		evaluationService.preselect(hrId(), app, Decision.VALIDEE, null);

		bookExam(app);
		assertThatThrownBy(() -> evaluationService.evaluateTechnical(expertId(), app,
				new TechnicalEvaluationRequest(Decision.VALIDEE, null,
						List.of(new Score(aTrait().getId(), (short) 11)))))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "NOTE_OUT_OF_RANGE");
	}

	@Test
	void theCvIsReadableByTheExpertTheExamWasHandedTo() {
		Integer app = applyToFreshOffer("ev13@example.fr");
		evaluationService.preselect(hrId(), app, Decision.VALIDEE, null);
		bookExam(app);

		assertThat(evaluationService.loadCv(expertId(), app).exists()).isTrue();
	}

	@Test
	void anotherExpertCannotReadTheCv() {
		Integer app = applyToFreshOffer("ev14@example.fr");
		evaluationService.preselect(hrId(), app, Decision.VALIDEE, null);
		bookExam(app);

		Integer other = settingsService.provision(new ProvisionAccountRequest(
				"expert.cv@bridge.local", "Autre", "Expert", "Motdepasse1!x", Role.EXPERT)).id();

		assertThatThrownBy(() -> evaluationService.loadCv(other, app))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "RESOURCE_NOT_FOUND");
	}

	@Test
	void theTechnicalGridExposesTheOffersTraits() {
		Integer app = applyToFreshOffer("ev12@example.fr");
		evaluationService.preselect(hrId(), app, Decision.VALIDEE, null);

		bookExam(app);
		assertThat(evaluationService.technicalContext(expertId(), app).traits())
				.anySatisfy(t -> assertThat(t.traitId()).isEqualTo(aTrait().getId()));
	}

}
