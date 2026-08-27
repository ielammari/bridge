package io.github.ielammari.bridge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import io.github.ielammari.bridge.dto.FinalEvaluationRequest;
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
import io.github.ielammari.bridge.model.JobOffer;
import io.github.ielammari.bridge.model.Trait;
import io.github.ielammari.bridge.repository.AppointmentRepository;
import io.github.ielammari.bridge.repository.JobOfferRepository;
import io.github.ielammari.bridge.repository.TraitRepository;
import io.github.ielammari.bridge.repository.UserRepository;

/**
 * An offer decides whether its evaluators are held to the hour booked for an
 * interview. The boundary is exercised directly, since the scheduler refuses a
 * slot in the past.
 */
@SpringBootTest
@Transactional
class EvaluationTimingTest {

	private static final LocalDate SEED = LocalDate.of(2099, 3, 4);
	private static final LocalTime HOUR = LocalTime.of(10, 0);
	private static final LocalTime LATER = LocalTime.of(11, 0);

	@Autowired private AuthService authService;
	@Autowired private ProfileService profileService;
	@Autowired private OfferService offerService;
	@Autowired private ApplicationService applicationService;
	@Autowired private EvaluationService evaluationService;
	@Autowired private AppointmentService appointmentService;
	@Autowired private AppointmentRepository appointments;
	@Autowired private JobOfferRepository offers;
	@Autowired private TraitRepository traits;
	@Autowired private UserRepository users;

	// ---- The rule itself ------------------------------------------------

	@Test
	void anOfferThatWaivesTheRuleIsOpenBeforeTheHour() {
		LocalDateTime wellBefore = LocalDateTime.of(2099, 3, 4, 8, 0);
		assertThat(Timing.isOpen(false, SEED, HOUR, wellBefore)).isTrue();
	}

	@Test
	void theHourItselfCounts() {
		assertThat(Timing.isOpen(true, SEED, HOUR, LocalDateTime.of(2099, 3, 4, 9, 59))).isFalse();
		assertThat(Timing.isOpen(true, SEED, HOUR, LocalDateTime.of(2099, 3, 4, 10, 0))).isTrue();
		assertThat(Timing.isOpen(true, SEED, HOUR, LocalDateTime.of(2099, 3, 4, 10, 1))).isTrue();
	}

	@Test
	void thereIsNoClosingEdge() {
		assertThat(Timing.isOpen(true, SEED, HOUR, LocalDateTime.of(2099, 3, 8, 23, 0))).isTrue();
	}

	// ---- Through the funnel ---------------------------------------------

	@Test
	void anExamCannotBeRecordedBeforeItsHour() {
		Integer app = scheduledExam("t1@example.fr", true);
		Integer expert = expertId();

		assertThatThrownBy(() -> evaluationService.evaluateTechnical(expert, app, scored()))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "INTERVIEW_NOT_DUE");
	}

	@Test
	void theExpertStillSeesTheExamAndItsGrid() {
		Integer app = scheduledExam("t2@example.fr", true);
		Integer expert = expertId();

		assertThat(evaluationService.pendingTechnical(expert))
				.extracting("applicationId").contains(app);
		assertThatCode(() -> evaluationService.technicalContext(expert, app)).doesNotThrowAnyException();
		assertThat(evaluationService.technicalContext(expert, app).waitForAppointment()).isTrue();
	}

	@Test
	void anOfferThatWaivesTheRuleRecordsRightAway() {
		Integer app = scheduledExam("t3@example.fr", false);

		assertThatCode(() -> evaluationService.evaluateTechnical(expertId(), app, scored()))
				.doesNotThrowAnyException();
	}

	@Test
	void theFinalInterviewCannotBeRecordedBeforeItsHour() {
		Integer app = scheduledExam("t4@example.fr", true);
		// Move it past the exam so the HR interview is the appointment in play.
		offerOf(app).setWaitForAppointment(false);
		evaluationService.evaluateTechnical(expertId(), app, scored());
		offerOf(app).setWaitForAppointment(true);
		appointmentService.schedule(hrId(), app, freeDay(hrId(), LATER), LATER, null);

		assertThatThrownBy(() -> evaluationService.finalize(hrId(), app, finalDecision()))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "INTERVIEW_NOT_DUE");
	}

	// ---- Fixtures --------------------------------------------------------

	private Integer hrId() {
		return users.findByEmailIgnoreCase("rh@bridge.local").orElseThrow().getId();
	}

	private Integer expertId() {
		return users.findByEmailIgnoreCase("expert@bridge.local").orElseThrow().getId();
	}

	private Trait aTrait() {
		return traits.findAll().get(0);
	}

	/** The offer behind an application, so a test can flip its rule mid funnel. */
	private JobOffer offerOf(Integer applicationId) {
		return offers.findByIdWithRequirements(
				applicationService.hrView(hrId(), applicationId).offerId()).orElseThrow();
	}

	private Integer candidate(String email) {
		Integer id = authService.register(new RegisterRequest(email, "Motdepasse1!x", "Ada", "Lovelace",
				"0612345678", LocalDate.of(1995, 5, 20), null, "Lyon", "France")).user().id();
		profileService.update(id, new UpdateProfileRequest(Degree.BAC_5, null,
				List.of(new TraitSelection(aTrait().getId(), null))));
		profileService.storeCv(id,
				new MockMultipartFile("file", "cv.pdf", "application/pdf", "%PDF-1.4".getBytes()), null);
		return id;
	}

	/** An application screened through and booked for an exam that has not happened. */
	private Integer scheduledExam(String email, boolean waits) {
		Integer offer = offerService.create(hrId(), new OfferRequest("Poste", null, "d", Degree.BAC,
				ContractType.PERMANENT, "Paris", null, null, null,
				waits, List.of(new RequirementSelection(aTrait().getId(), true)), true)).id();
		Integer app = applicationService.apply(candidate(email), offer, null).id();
		evaluationService.preselect(hrId(), app, Decision.VALIDEE, null);
		appointmentService.schedule(hrId(), app, freeDay(expertId(), HOUR), HOUR, expertId());
		return app;
	}

	/**
	 * The first day from the seed on which that evaluator holds nothing at that
	 * hour. The development database carries bookings of its own.
	 */
	private LocalDate freeDay(Integer evaluatorId, LocalTime hour) {
		LocalDate day = SEED;
		while (appointments.existsByEvaluatorIdAndDateAndTime(evaluatorId, day, hour)) {
			day = day.plusDays(1);
		}
		return day;
	}

	private TechnicalEvaluationRequest scored() {
		return new TechnicalEvaluationRequest(Decision.VALIDEE, "Solide",
				List.of(new Score(aTrait().getId(), (short) 8)));
	}

	private FinalEvaluationRequest finalDecision() {
		return new FinalEvaluationRequest(Decision.REFUSEE, "Bilan",
				new FinalEvaluationRequest.InterviewData(null, null, null, null, null, null, null),
				null);
	}

}
