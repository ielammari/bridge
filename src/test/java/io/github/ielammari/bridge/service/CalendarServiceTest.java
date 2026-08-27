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

import io.github.ielammari.bridge.dto.CalendarDto;
import io.github.ielammari.bridge.dto.CalendarEntryDto;
import io.github.ielammari.bridge.dto.OfferRequest;
import io.github.ielammari.bridge.dto.OfferRequest.RequirementSelection;
import io.github.ielammari.bridge.dto.ProvisionAccountRequest;
import io.github.ielammari.bridge.dto.RegisterRequest;
import io.github.ielammari.bridge.dto.TechnicalEvaluationRequest;
import io.github.ielammari.bridge.dto.TechnicalEvaluationRequest.Score;
import io.github.ielammari.bridge.dto.UpdateProfileRequest;
import io.github.ielammari.bridge.dto.UpdateProfileRequest.TraitSelection;
import io.github.ielammari.bridge.exception.ApiException;
import io.github.ielammari.bridge.model.CalendarScope;
import io.github.ielammari.bridge.model.ContractType;
import io.github.ielammari.bridge.model.Decision;
import io.github.ielammari.bridge.model.Degree;
import io.github.ielammari.bridge.model.OrganisationSettings;
import io.github.ielammari.bridge.model.Role;
import io.github.ielammari.bridge.model.Trait;
import io.github.ielammari.bridge.repository.AppointmentRepository;
import io.github.ielammari.bridge.repository.OrganisationSettingsRepository;
import io.github.ielammari.bridge.repository.TraitRepository;
import io.github.ielammari.bridge.repository.UserRepository;

/**
 * A calendar belongs to the evaluator who runs it. A recruiter also reads the
 * exams they arranged and the calendar of an expert they are booking; anything
 * else answers as though it did not exist.
 */
@SpringBootTest
@Transactional
class CalendarServiceTest {

	private static final LocalTime HOUR = LocalTime.of(11, 0);
	private static final LocalDate SEED = LocalDate.of(2099, 6, 1);

	@Autowired private AuthService authService;
	@Autowired private ProfileService profileService;
	@Autowired private OfferService offerService;
	@Autowired private ApplicationService applicationService;
	@Autowired private EvaluationService evaluationService;
	@Autowired private AppointmentService appointmentService;
	@Autowired private CalendarService calendarService;
	@Autowired private SettingsService settingsService;
	@Autowired private AppointmentRepository appointments;
	@Autowired private OrganisationSettingsRepository organisation;
	@Autowired private TraitRepository traits;
	@Autowired private UserRepository users;

	@Test
	void anEvaluatorSeesTheInterviewsTheyRunAndNobodyElses() {
		LocalDate day = freeDay(expertId(), HOUR);
		Integer app = examFor(expertId(), "cal1@example.fr", day);

		assertThat(oneDay(expertId(), CalendarScope.MINE, null, day).entries())
				.extracting("applicationId").contains(app);
		assertThat(oneDay(secondExpert(), CalendarScope.MINE, null, day).entries())
				.extracting("applicationId").doesNotContain(app);
	}

	@Test
	void anExpertCannotOpenAnotherEvaluatorsCalendar() {
		Integer reader = expertId();
		Integer other = secondExpert();
		LocalDate day = SEED;

		assertThatThrownBy(() -> oneDay(reader, CalendarScope.EVALUATOR, other, day))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "RESOURCE_NOT_FOUND");
	}

	@Test
	void anExpertCannotAskForWhatARecruiterPlanned() {
		Integer reader = expertId();
		LocalDate day = SEED;

		assertThatThrownBy(() -> oneDay(reader, CalendarScope.PLANNED, null, day))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "RESOURCE_NOT_FOUND");
	}

	@Test
	void aRecruiterSeesTheExamsTheyPlannedAndNotAnotherRecruiters() {
		LocalDate day = freeDay(expertId(), HOUR);
		Integer mine = examFor(expertId(), "cal2@example.fr", day);
		Integer theirs = examOf(otherRecruiter(), secondExpert(), "cal3@example.fr",
				freeDay(secondExpert(), HOUR));

		List<?> planned = oneDay(hrId(), CalendarScope.PLANNED, null, day).entries();
		assertThat(planned).extracting("applicationId").contains(mine);
		assertThat(planned).extracting("applicationId").doesNotContain(theirs);
	}

	@Test
	void aRecruiterOpensTheCalendarOfTheExpertTheyAreBooking() {
		LocalDate day = freeDay(secondExpert(), HOUR);
		Integer app = examFor(secondExpert(), "cal4@example.fr", day);

		assertThat(oneDay(hrId(), CalendarScope.EVALUATOR, secondExpert(), day).entries())
				.extracting("applicationId").contains(app);
	}

	@Test
	void aRangeWiderThanTheLimitIsRejected() {
		Integer reader = expertId();

		assertThatThrownBy(() -> calendarService.read(reader, CalendarScope.MINE, null,
				SEED, SEED.plusYears(1)))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "RANGE_INVALID");
		assertThatThrownBy(() -> calendarService.read(reader, CalendarScope.MINE, null,
				SEED, SEED.minusDays(1)))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "RANGE_INVALID");
	}

	@Test
	void anExamIsRecordedOnceItsResultIsIn() {
		LocalDate day = freeDay(expertId(), HOUR);
		Integer app = examFor(expertId(), "cal5@example.fr", day);

		assertThat(entryFor(app, day)).returns(false, e -> e.recorded());

		evaluationService.evaluateTechnical(expertId(), app, scored());

		assertThat(entryFor(app, day)).returns(true, e -> e.recorded());
	}

	@Test
	void theCalendarReportsTheBookableHoursOfADay() {
		OrganisationSettings grid = organisation.findById(OrganisationSettings.SINGLETON_ID)
				.orElseThrow();

		assertThat(oneDay(expertId(), CalendarScope.MINE, null, SEED).capacity())
				.isEqualTo(grid.getLastHour() - grid.getFirstHour() + 1);
	}

	// ---- Fixtures --------------------------------------------------------

	private CalendarDto oneDay(Integer callerId, CalendarScope scope, Integer evaluatorId,
			LocalDate day) {
		return calendarService.read(callerId, scope, evaluatorId, day, day);
	}

	private CalendarEntryDto entryFor(Integer applicationId, LocalDate day) {
		return oneDay(expertId(), CalendarScope.MINE, null, day).entries().stream()
				.filter(e -> e.applicationId().equals(applicationId))
				.findFirst().orElseThrow();
	}

	private Integer hrId() {
		return users.findByEmailIgnoreCase("rh@bridge.local").orElseThrow().getId();
	}

	private Integer expertId() {
		return users.findByEmailIgnoreCase("expert@bridge.local").orElseThrow().getId();
	}

	private Integer secondExpert() {
		return users.findByEmailIgnoreCase("second.expert@bridge.local")
				.map(account -> account.getId())
				.orElseGet(() -> settingsService.provision(new ProvisionAccountRequest(
						"second.expert@bridge.local", "Yann", "Moreau", "Evaluation2026!x",
						Role.EXPERT)).id());
	}

	private Integer otherRecruiter() {
		return users.findByEmailIgnoreCase("second.rh@bridge.local")
				.map(account -> account.getId())
				.orElseGet(() -> settingsService.provision(new ProvisionAccountRequest(
						"second.rh@bridge.local", "Claire", "Fontaine", "Recrutement2026!x",
						Role.RH)).id());
	}

	private Trait aTrait() {
		return traits.findAll().get(0);
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

	private Integer examFor(Integer expert, String email, LocalDate day) {
		return examOf(hrId(), expert, email, day);
	}

	/** An exam that recruiter arranged with that expert, on an offer of their own. */
	private Integer examOf(Integer recruiter, Integer expert, String email, LocalDate day) {
		Integer offer = offerService.create(recruiter, new OfferRequest("Poste", null, "d", Degree.BAC,
				ContractType.PERMANENT, "Paris", null, null, null,
				false, List.of(new RequirementSelection(aTrait().getId(), true)), true)).id();
		Integer app = applicationService.apply(candidate(email), offer, null).id();
		evaluationService.preselect(recruiter, app, Decision.VALIDEE, null);
		appointmentService.schedule(recruiter, app, day, HOUR, expert);
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

}
