package io.github.ielammari.bridge.service;

import static org.assertj.core.api.Assertions.assertThat;
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

import io.github.ielammari.bridge.dto.OfferRequest;
import io.github.ielammari.bridge.dto.OfferRequest.RequirementSelection;
import io.github.ielammari.bridge.dto.ProvisionAccountRequest;
import io.github.ielammari.bridge.dto.RegisterRequest;
import io.github.ielammari.bridge.dto.TechnicalEvaluationRequest;
import io.github.ielammari.bridge.dto.TechnicalEvaluationRequest.Score;
import io.github.ielammari.bridge.dto.UpdateProfileRequest;
import io.github.ielammari.bridge.dto.UpdateProfileRequest.TraitSelection;
import io.github.ielammari.bridge.exception.ApiException;
import io.github.ielammari.bridge.model.ContractType;
import io.github.ielammari.bridge.model.Decision;
import io.github.ielammari.bridge.model.Degree;
import io.github.ielammari.bridge.model.NotificationType;
import io.github.ielammari.bridge.model.Role;
import io.github.ielammari.bridge.model.Trait;
import io.github.ielammari.bridge.repository.TraitRepository;
import io.github.ielammari.bridge.repository.UserRepository;

/**
 * HR hands each exam to a named expert. The exam is that expert's work and
 * nobody else's, and it reaches them only once it has an hour.
 */
@SpringBootTest
@Transactional
class ExamAssignmentTest {

	@Autowired private AuthService authService;
	@Autowired private ProfileService profileService;
	@Autowired private OfferService offerService;
	@Autowired private ApplicationService applicationService;
	@Autowired private EvaluationService evaluationService;
	@Autowired private AppointmentService appointmentService;
	@Autowired private MessageService messageService;
	@Autowired private PeopleService peopleService;
	@Autowired private SettingsService settingsService;
	@Autowired private TraitRepository traits;
	@Autowired private UserRepository users;

	private Integer hrId() {
		return users.findByEmailIgnoreCase("rh@bridge.local").orElseThrow().getId();
	}

	private Integer firstExpert() {
		return users.findByEmailIgnoreCase("expert@bridge.local").orElseThrow().getId();
	}

	private Integer secondExpert() {
		return users.findByEmailIgnoreCase("second.expert@bridge.local")
				.map(account -> account.getId())
				.orElseGet(() -> settingsService.provision(new ProvisionAccountRequest(
						"second.expert@bridge.local", "Yann", "Moreau", "Evaluation2026!x",
						Role.EXPERT)).id());
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

	private Integer publishedOffer() {
		return offerService.create(hrId(), new OfferRequest("Poste", null, "d", Degree.BAC,
				ContractType.PERMANENT, "Paris", null, null, null,
				List.of(new RequirementSelection(aTrait().getId(), true)), true)).id();
	}

	/** An application screened through to the exam stage, not yet scheduled. */
	private Integer awaitingExam(String email) {
		Integer app = applicationService.apply(candidate(email), publishedOffer(), null).id();
		evaluationService.preselect(hrId(), app, Decision.VALIDEE, null);
		return app;
	}

	private TechnicalEvaluationRequest scored() {
		return new TechnicalEvaluationRequest(Decision.VALIDEE, "Solide",
				List.of(new Score(aTrait().getId(), (short) 8)));
	}

	@Test
	void anUnscheduledExamReachesNobody() {
		Integer app = awaitingExam("x1@example.fr");

		assertThat(evaluationService.pendingTechnical(firstExpert()))
				.extracting("applicationId").doesNotContain(app);
		assertThat(evaluationService.pendingTechnical(secondExpert()))
				.extracting("applicationId").doesNotContain(app);
	}

	@Test
	void aScheduledExamReachesTheExpertItWasHandedTo() {
		Integer app = awaitingExam("x2@example.fr");
		appointmentService.schedule(hrId(), app, LocalDate.of(2099, 2, 1), LocalTime.of(9, 0),
				secondExpert());

		assertThat(evaluationService.pendingTechnical(secondExpert()))
				.extracting("applicationId").contains(app);
		assertThat(evaluationService.pendingTechnical(firstExpert()))
				.extracting("applicationId").doesNotContain(app);
	}

	@Test
	void anExpertCannotSitSomebodyElsesExam() {
		Integer app = awaitingExam("x3@example.fr");
		appointmentService.schedule(hrId(), app, LocalDate.of(2099, 2, 2), LocalTime.of(9, 0),
				secondExpert());
		Integer other = firstExpert();

		assertThatThrownBy(() -> evaluationService.technicalContext(other, app))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "RESOURCE_NOT_FOUND");
		assertThatThrownBy(() -> evaluationService.evaluateTechnical(other, app, scored()))
				.hasFieldOrPropertyWithValue("code", "RESOURCE_NOT_FOUND");
	}

	@Test
	void anExamThatWasNeverScheduledCannotBeEvaluated() {
		Integer app = awaitingExam("x4@example.fr");
		Integer expert = firstExpert();

		assertThatThrownBy(() -> evaluationService.evaluateTechnical(expert, app, scored()))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "RESOURCE_NOT_FOUND");
	}

	@Test
	void schedulingAnExamWithoutNamingAnExpertIsRejected() {
		Integer app = awaitingExam("x5@example.fr");
		Integer hr = hrId();

		assertThatThrownBy(() -> appointmentService.schedule(hr, app, LocalDate.of(2099, 2, 3),
				LocalTime.of(9, 0), null))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "EXPERT_REQUIRED");
	}

	@Test
	void twoExpertsMayHoldTheSameHourAndNeitherMayHoldTwo() {
		Integer first = awaitingExam("x6@example.fr");
		Integer second = awaitingExam("x7@example.fr");
		LocalDate day = LocalDate.of(2099, 2, 4);
		appointmentService.schedule(hrId(), first, day, LocalTime.of(10, 0), firstExpert());

		appointmentService.schedule(hrId(), second, day, LocalTime.of(10, 0), secondExpert());

		Integer third = awaitingExam("x8@example.fr");
		Integer hr = hrId();
		Integer busy = firstExpert();
		assertThatThrownBy(() -> appointmentService.schedule(hr, third, day, LocalTime.of(10, 0), busy))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "SLOT_TAKEN");
	}

	@Test
	void handingAnExamOnTellsBothExperts() {
		Integer app = awaitingExam("x9@example.fr");
		appointmentService.schedule(hrId(), app, LocalDate.of(2099, 2, 5), LocalTime.of(9, 0),
				firstExpert());

		appointmentService.schedule(hrId(), app, LocalDate.of(2099, 2, 6), LocalTime.of(9, 0),
				secondExpert());

		assertThat(messageService.inbox(firstExpert()))
				.anySatisfy(m -> assertThat(m.type()).isEqualTo(NotificationType.EXAM_UNASSIGNED));
		assertThat(evaluationService.pendingTechnical(firstExpert()))
				.extracting("applicationId").doesNotContain(app);
		assertThat(evaluationService.pendingTechnical(secondExpert()))
				.extracting("applicationId").contains(app);
	}

	@Test
	void theExpertHoldingAnExamOpensThatCandidateAndOnlyThatApplication() {
		Integer id = candidate("x10@example.fr");
		Integer app = applicationService.apply(id, publishedOffer(), null).id();
		evaluationService.preselect(hrId(), app, Decision.VALIDEE, null);
		applicationService.apply(id, publishedOffer(), null);
		appointmentService.schedule(hrId(), app, LocalDate.of(2099, 2, 7), LocalTime.of(9, 0),
				secondExpert());

		assertThat(peopleService.dossier(secondExpert(), Role.EXPERT, id).applications())
				.singleElement()
				.satisfies(row -> assertThat(row.id()).isEqualTo(app));
		Integer stranger = firstExpert();
		assertThatThrownBy(() -> peopleService.dossier(stranger, Role.EXPERT, id))
				.hasFieldOrPropertyWithValue("code", "RESOURCE_NOT_FOUND");
	}

	/** The grace period, so an exam is late only once it is really late. */
	@Test
	void anExamCountsAsUnattendedOnlyAfterTheGracePeriod() {
		LocalDate day = LocalDate.of(2026, 3, 4);
		LocalTime hour = LocalTime.of(10, 0);

		assertThat(ExamWatch.isUnattended(day, hour, LocalDateTime.of(day, LocalTime.of(12, 59)))).isFalse();
		assertThat(ExamWatch.isUnattended(day, hour, LocalDateTime.of(day, LocalTime.of(13, 1)))).isTrue();
	}

}
